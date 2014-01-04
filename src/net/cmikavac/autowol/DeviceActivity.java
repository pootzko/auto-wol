package net.cmikavac.autowol;

import net.cmikavac.autowol.TimePickerFragment.OnTimePickedListener;
import net.cmikavac.autowol.models.Device;
import net.cmikavac.autowol.utils.TimeConverter;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class DeviceActivity extends BaseActivity implements OnTimePickedListener {
    private Device mDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        setDevice();
        setViewValues();
        registerSwitchCallbacks();
        registerLinearLayoutButtonsCallbacks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        menu.findItem(R.id.action_new).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            case R.id.action_save:
                getViewValues();
                saveDeviceToDb();
                this.finish();
                break;
            case R.id.action_new:
                break;
        }
        return true;
    }

    private void registerSwitchCallbacks() {
        final CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                switch (button.getId()) {
                    case R.id.switch_auto_wake:
                        toggleLinearLayoutVisibility(R.id.layout_auto_wake, isChecked);
                        break;
                    case R.id.switch_quiet_hours:
                        toggleLinearLayoutVisibility(R.id.layout_quiet_hours, isChecked);
                        break;
                    case R.id.switch_idle_time:
                        toggleLinearLayoutVisibility(R.id.layout_idle_time, isChecked);
                        break;
                }
            }
        };

        ItemHolder itemHolder = createItemHolder();
        itemHolder.autoWakeSwitch.setOnCheckedChangeListener(listener);
        itemHolder.quietHoursSwitch.setOnCheckedChangeListener(listener);
        itemHolder.idleTimeSwitch.setOnCheckedChangeListener(listener);
    }

    private void registerLinearLayoutButtonsCallbacks() {
        final OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog(view.getId());
            }
        };

        ItemHolder itemHolder = createItemHolder();
        itemHolder.quietHoursFromLayout.setOnClickListener(listener);
        itemHolder.quietHoursToLayout.setOnClickListener(listener);
    }

    private void toggleLinearLayoutVisibility(int layoutId, boolean isChecked) {
        LinearLayout linearLayout = (LinearLayout)findViewById(layoutId);
        if (isChecked == true) {
            linearLayout.setVisibility(LinearLayout.VISIBLE);
        }
        else {
            linearLayout.setVisibility(LinearLayout.GONE);
        }
    }

    public void showTimePickerDialog(int layoutId) {
        int hour = getQuietHoursHour(layoutId);
        int minute = getQuietHoursMinute(layoutId);

        Bundle bundle = createTimePickerBundle(layoutId, hour, minute);
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), Integer.toString(layoutId));
    }

    public int getQuietHoursHour(int layoutId) {
        int hour = layoutId  == R.id.layout_quiet_hours_from ? 0 : 7;

        switch (layoutId) {
            case R.id.layout_quiet_hours_from:
                if (mDevice.getQuietHoursFrom() != null)
                    hour = TimeConverter.getHourFromMilliseconds(mDevice.getQuietHoursFrom());
                break;
            case R.id.layout_quiet_hours_to:
                if (mDevice.getQuietHoursTo() != null)
                    hour = TimeConverter.getHourFromMilliseconds(mDevice.getQuietHoursTo());
                break;
        }

        return hour;
    }

    public int getQuietHoursMinute(int layoutId) {
        int minute = 0;

        switch (layoutId) {
            case R.id.layout_quiet_hours_from:
                if (mDevice.getQuietHoursFrom() != null)
                    minute = TimeConverter.getMinuteFromMilliseconds(mDevice.getQuietHoursFrom());
                break;
            case R.id.layout_quiet_hours_to:
                if (mDevice.getQuietHoursTo() != null)
                    minute = TimeConverter.getMinuteFromMilliseconds(mDevice.getQuietHoursTo());
                break;
        }

        return minute;
    }

    public Bundle createTimePickerBundle(int layoutId, int hour, int minute) {
        Bundle bundle = new Bundle();
        bundle.putInt("layoutId", layoutId);
        bundle.putInt("hour", hour);
        bundle.putInt("minute", minute);
        return bundle;
    }

    @Override
    public void onTimePicked(int layoutId, int hour, int minute) {
        Long timeInMillis = TimeConverter.getTimeInMilliseconds(hour, minute);
        Integer textId = setDeviceQuietHoursValues(layoutId, timeInMillis);
        setLayoutQuietHoursText(timeInMillis, textId);
    }

    private Integer setDeviceQuietHoursValues(int layoutId, Long timeInMillis) {
        Integer textId = null;

        switch (layoutId) {
            case R.id.layout_quiet_hours_from:
                textId = R.id.text_quiet_hours_from;
                mDevice.setQuietHoursFrom(timeInMillis);
                break;
            case R.id.layout_quiet_hours_to:
                textId = R.id.text_quiet_hours_to;
                mDevice.setQuietHoursTo(timeInMillis);
                break;
        }

        return textId;
    }

    private void setLayoutQuietHoursText(Long timeInMillis, Integer textId) {
        TextView textView = (TextView)findViewById(textId);
        textView.setText(TimeConverter.getFormatedTime(timeInMillis, this));
    }

    private void setDevice() {
        Intent intent = getIntent();
        Device device = (Device)intent.getSerializableExtra("deviceObject");
        
        if (device == null) {
            mDevice = new Device();
        }
        else {
            mDevice = device;
        }
    }

    private void saveDeviceToDb() {
        if (mDevice.getId() == -1) {
            mDbProvider.insertDevice(mDevice);
        }
        else {
            mDbProvider.updateDevice(mDevice);
        }
    }

    private void setViewValues() {
        ItemHolder itemHolder = createItemHolder();

        itemHolder.nameEdit.setText(mDevice.getName());
        itemHolder.macEdit.setText(mDevice.getMac());
        

        if (mDevice.getIp() != null) {
            itemHolder.ipEdit.setText(mDevice.getIp());
        }

        if (mDevice.getPort() != null) {
            itemHolder.portEdit.setText(mDevice.getPort());
        }

        if (mDevice.getIdleTime() != null) {
            itemHolder.idleTimeEdit.setText(mDevice.getIdleTime());
        }
        
        if (mDevice.getSSID() != null) {
            itemHolder.autoWakeSwitch.setChecked(true);
        }
        else {
            itemHolder.autoWakeLayout.setVisibility(LinearLayout.GONE);
        }

        if (mDevice.getQuietHoursFrom() != null) {
            itemHolder.quietHoursSwitch.setChecked(true);
        }
        else {
            itemHolder.quietHoursLayout.setVisibility(LinearLayout.GONE);
        }

        if (mDevice.getIdleTime() != null) {
            itemHolder.idleTimeSwitch.setChecked(true);
        }
        else {
            itemHolder.idleTimeLayout.setVisibility(LinearLayout.GONE);
        }
    }

    private void getViewValues() {
        ItemHolder itemHolder = createItemHolder();

        mDevice.setName(itemHolder.nameEdit.getText().toString());
        mDevice.setIp(itemHolder.ipEdit.getText().toString());
        mDevice.setMac(itemHolder.macEdit.getText().toString());
    }

    private ItemHolder createItemHolder() {
        ItemHolder itemHolder = new ItemHolder();
        
        // EditText
        itemHolder.nameEdit = (EditText)findViewById(R.id.edit_name);
        itemHolder.ipEdit = (EditText)findViewById(R.id.edit_ip);
        itemHolder.macEdit = (EditText)findViewById(R.id.edit_mac);
        itemHolder.portEdit = (EditText)findViewById(R.id.edit_port);
        itemHolder.portEdit = (EditText)findViewById(R.id.edit_ssid);
        itemHolder.idleTimeEdit = (EditText)findViewById(R.id.edit_idle_time);
        
        // TextView
        itemHolder.quietHoursFromText = (TextView)findViewById(R.id.text_quiet_hours_from);
        itemHolder.quietHoursToText = (TextView)findViewById(R.id.text_quiet_hours_to);
        
        // LinearLayout
        itemHolder.autoWakeLayout = (LinearLayout)findViewById(R.id.layout_auto_wake);
        itemHolder.quietHoursLayout = (LinearLayout)findViewById(R.id.layout_quiet_hours);
        itemHolder.quietHoursFromLayout = (LinearLayout)findViewById(R.id.layout_quiet_hours_from);
        itemHolder.quietHoursToLayout = (LinearLayout)findViewById(R.id.layout_quiet_hours_to);
        itemHolder.idleTimeLayout = (LinearLayout)findViewById(R.id.layout_idle_time);
        
        // Switch
        itemHolder.autoWakeSwitch = (Switch)findViewById(R.id.switch_auto_wake);
        itemHolder.quietHoursSwitch = (Switch)findViewById(R.id.switch_quiet_hours);
        itemHolder.idleTimeSwitch = (Switch)findViewById(R.id.switch_idle_time);

        return itemHolder;
    }

    private class ItemHolder {
        // EditText
        EditText nameEdit;
        EditText ipEdit;
        EditText macEdit;
        EditText portEdit;
        EditText ssidEdit;
        EditText idleTimeEdit;

        // TextView
        TextView quietHoursFromText;
        TextView quietHoursToText;

        // LinearLayout
        LinearLayout autoWakeLayout;
        LinearLayout quietHoursLayout;
        LinearLayout quietHoursFromLayout;
        LinearLayout quietHoursToLayout;
        LinearLayout idleTimeLayout;

        // Switch
        Switch autoWakeSwitch;
        Switch quietHoursSwitch;
        Switch idleTimeSwitch;
    }
}