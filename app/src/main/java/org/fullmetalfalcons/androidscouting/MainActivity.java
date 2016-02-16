package org.fullmetalfalcons.androidscouting;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import org.fullmetalfalcons.androidscouting.bluetooth.BluetoothCore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private final Pattern bluetoothCodePattern = Pattern.compile("([a-fA-F]|\\d){4}");
    private BroadcastReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText bluetoothCodeView = (EditText) findViewById(R.id.bluetoothCode);

        //Refresh button next to the bluetooth code EditText
        final ImageButton refreshBtn = (ImageButton) findViewById(R.id.detail_refresh_btn);
        //The animation to make the button spin
        final Animation ranim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        //When the refresh button is pressed
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create a matcher based on the pattern above and the text in the code EditText
                Matcher bluetoothCodeMatcher = bluetoothCodePattern.matcher(bluetoothCodeView.getText());
                //If the text matches the pattern
                if (bluetoothCodeMatcher.matches()) {
                    //Animate the refresh button
                    refreshBtn.startAnimation(ranim);
                    //Set a new passphrase
                    BluetoothCore.setPassphrase(bluetoothCodeView.getText().toString());
                } else {
                    //Send error message to user
                    sendError("Bluetooth Code must be in the format (# or (A-F))x4", false);
                    //Clear the code box
                    bluetoothCodeView.setText("");
                }

            }
        });


        //Start advertising
        BluetoothCore.startBLE(this);


        //Register broadcast reciever to detect changes to bluetooth adapter
        registerBluetoothReceiver();
    }

    /**
     * Changes the color of connected notification icon based on connection state
     *
     * @param connected whether or not a bluetooth device is connected
     */
    public void setConnected(final boolean connected){
        //Updates to UI must be run on the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Get connection indicator
                View v = findViewById(R.id.connection_indicator);

                //Get the shape itself
                LayerDrawable bgDrawable = (LayerDrawable) v.getBackground();
                //Set inner circle color
                GradientDrawable shape = (GradientDrawable) bgDrawable.getDrawable(0);
                shape.setColor(ContextCompat.getColor(MainActivity.this, connected ? R.color.colorGreenIndicator : R.color.colorRedIndicator));

                //Set outer circle color
                shape = (GradientDrawable) bgDrawable.getDrawable(1);
                shape.setStroke(8, ContextCompat.getColor(MainActivity.this, connected ? R.color.colorGreenIndicator : R.color.colorRedIndicator));

            }
        });
        final ImageButton refreshBtn=(ImageButton)findViewById(R.id.detail_refresh_btn);
        refreshBtn.setEnabled(!connected);

    }

    /**
     * Changes the color of connected notification icon based on connection state
     *
     * @param advertising whether or not a bluetooth device is connected
     */
    public void setAdvertising(final boolean advertising) {
        //Updates to UI must be run on the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Get connection indicator
                View v = findViewById(R.id.advertising_indicator);

                //Get the shape itself
                LayerDrawable bgDrawable = (LayerDrawable) v.getBackground();
                //Set inner circle color
                GradientDrawable shape = (GradientDrawable) bgDrawable.getDrawable(0);
                shape.setColor(ContextCompat.getColor(MainActivity.this, advertising ? R.color.colorGreenIndicator : R.color.colorRedIndicator));

                //Set outer circle color
                shape = (GradientDrawable) bgDrawable.getDrawable(1);
                shape.setStroke(8, ContextCompat.getColor(MainActivity.this, advertising ? R.color.colorGreenIndicator : R.color.colorRedIndicator));
            }
        });

    }


    /**
     * Sends a popup message to the user with a custom message.
     * Also closes the app if the error is fatal
     *
     * @param message message to send to the user
     * @param fatalError whether or not the app should close after user acknowledges
     */
    public void sendError(String message,final boolean fatalError){
        new AlertDialog.Builder(this)
                .setTitle("Something is wrong")
                        //Can ignore if not fatal
                .setCancelable(!fatalError)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Close the app
                        if (fatalError) {
                            System.exit(0);
                        }
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        if(fatalError){
            Log.wtf(getString(R.string.log_tag), message);
        } else {
            Log.e(getString(R.string.log_tag),message);

        }
    }


    /**
     * Called when request for bluetooth permissions returns
     *
     * @param requestCode Number of activity request
     * @param resultCode Result of the activity
     * @param data data passed by the activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Value of Bluetooth Request Code is 1
        if ((requestCode == 1) && (resultCode == RESULT_OK)) {
            BluetoothCore.enable();
        }

        if ((requestCode == 1) && (resultCode == RESULT_CANCELED)) {
            sendError("Bluetooth must be enabled for this app to function", true);
        }
    }

    public void startScoutActivity(View view){
        Intent intent = new Intent(this,ScoutingActivity.class);
        startActivity(intent);
    }

    /**
     * Creates and registers a BroadcastReceiver to track changes to BluetoothAdapter
     */
    private void registerBluetoothReceiver() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            sendError("Bluetooth is required for this app, don't turn it off",true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            break;
                        case BluetoothAdapter.STATE_ON:
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}