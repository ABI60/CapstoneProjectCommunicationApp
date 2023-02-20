package com.example.CapstoneProjectCommunicationApp;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;

@SuppressLint("all")
@SuppressWarnings("all")

public class SendReceiveActivity extends AppCompatActivity {

    //region -Create class variables
    // Gui variables
    private TextView logText;
    private TextView connectedDeviceText;
    private TextView leftDistanceTxt;
    private TextView rightDistanceTxt;
    private TextView batteryVoltageTxt;
    private Button clearLogsBtn;
    private Button startBtn;
    private Button stopBtn;

    // Create activity handler variable
    static public Handler sendReceiveHandler;

    // Create named constants to carry a message
    private final int Message_Received_Call = 1;
    //endregion

    private int received_byte;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_recieve);

        //region -Assign GUI variables
        logText = (TextView) findViewById(R.id.logText);
        connectedDeviceText = (TextView) findViewById(R.id.connectedDeviceText);
        clearLogsBtn = (Button)findViewById(R.id.clearLogsBtn);
        rightDistanceTxt = (TextView) findViewById(R.id.rightDistanceTxt);
        leftDistanceTxt = (TextView) findViewById(R.id.leftDistanceTxt);
        batteryVoltageTxt = (TextView) findViewById(R.id.batteryVoltageTxt);
        startBtn = (Button)findViewById(R.id.startBtn);
        stopBtn = (Button)findViewById(R.id.stopBtn);
        //endregion

        //region -Change the logtext configurations
        // Change the logtext background color
        logText.setBackgroundColor(Color.parseColor("#C0C0C0"));
        // Make the logtext scrollable
        logText.setMovementMethod(new ScrollingMovementMethod());
        // Make it so touching the logtext will stop the parents from interfering with the scroll
        logText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // Check if the the touched object is logtext
                if (v.getId() == R.id.logText) {
                    // Stop the parents from interfering with the scroll
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    // Check when the user releases the touch
                    if(event.getAction() == MotionEvent.ACTION_UP){
                        // Allow the parents to interfere
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                // Return when the user has consumed the event
                return false;
            }
        });
        //endregion

        // Show the user connected device name
        connectedDeviceText.setText("(Connected to: " + BluetoothVariables.connectedDeviceName + ")");

        //region -Only do these if it's the first time activity is launching
        if (savedInstanceState == null) {
            // Tell user which device they are connected to in the beginning
            logText.append("\n***Connected to: " + BluetoothVariables.connectedDeviceName + "***");

            // Set the stop variables to false initially
            BluetoothVariables.stopReceiveThread = false;
            BluetoothVariables.stopContinuousThread = false;

            // Start receiving thread
            ReceiveThread receiveThread = new ReceiveThread();
            receiveThread.start();
        }
        //endregion

        //region -Only do these if the activity is restarting
        if (savedInstanceState != null) {
            // Recover the log text
            String savedData = savedInstanceState.getString("Saved Log");
            logText.setText(savedData);
        }
        //endregion

        //region -On click listener for the "clear logs" button
        clearLogsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the log text and inform the user
                logText.setText("***Cleared logs!***");
            }
        });
        //endregion

        //region -On click listener for the "STOP" button
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(255);
                logText.append("\n***Shutting down!***");
            }
        });
        //endregion

        //region -On click listener for the "START" button
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logText.append("\n***Starting the robot!***");
                sendData(254);
            }
        });
        //endregion

        //region -Initialize the handler
        // Create a send receive activity handler for updating gui elements etc.
        sendReceiveHandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                if(msg.what == Message_Received_Call){

                    //Data is "right distance information"
                    if(received_byte < 50){
                        //If wrong value is received
                        if(received_byte == 0){
                            rightDistanceTxt.setText("X cm");
                        }
                        else {
                            rightDistanceTxt.setText(received_byte + " cm");
                        }
                    }
                    //Data is "left distance information"
                    else if(received_byte < 100){
                        //If wrong value is received
                        if(received_byte == 50){
                            leftDistanceTxt.setText("X cm");
                        }
                        else {
                            leftDistanceTxt.setText(received_byte - 50 + " cm");
                        }
                    }
                    //Data is "battery voltage information"
                    else if(received_byte < 200){
                        int battery_voltage = received_byte - 100;

                        if(battery_voltage == 100) {
                            batteryVoltageTxt.setText("-,-V");
                        }
                        else{
                            batteryVoltageTxt.setText(battery_voltage / 10 + "," + battery_voltage % 10 + "V");
                        }
                    }
                    //Other messages of progression
                    else if(received_byte == 211){logText.append("\nDetected Stairs!");}
                    else if(received_byte == 212){logText.append("\nApproaching the stairs!");}
                    else if(received_byte == 213){logText.append("\nRaising the front!");}
                    else if(received_byte == 214){logText.append("\nApproaching the stairs again!");}
                    else if(received_byte == 215){logText.append("\nRaising the back!");}
                    else if(received_byte == 216){logText.append("\nMoving the back to platform!");}
                    else if(received_byte == 217){logText.append("\nNext stair detected!");}
                    else if(received_byte == 218){logText.append("\nStair end detected!");}
                    else if(received_byte == 219){logText.append("\nTurning left to detect stairs!");}
                    else if(received_byte == 250){logText.append("\n***Battery Low!***");}
                }
            }
        };
        //endregion

    }// End of the onCreate

    // Make a class that extends a thread class to pool for receiving data
    class ReceiveThread extends Thread{
        public void run() {
            // Loop until stop condition is set
            while(BluetoothVariables.stopReceiveThread == false){
                // Try to read the data
                try{
                    // Read the data(this function blocks until it receives something to read)
                    received_byte = BluetoothVariables.inStream.read();
                    // Inform the user that data was received
                    SendReceiveActivity.sendReceiveHandler.obtainMessage(Message_Received_Call).sendToTarget();
                }catch (IOException e){
                    // Coming down here means the connection was lost

                    // Send a message to the main handler
                    MainActivity.mainHandler.obtainMessage(3,5,-1).sendToTarget();
                    // Finish the activity
                    finish();
                    // Break out of the infinite while loop
                    break;
                }
            }
        }
    }

    // Class method to send the data
    public void sendData(int input) {
        // Try to send the data
        try {
            // Send the data
            BluetoothVariables.outStream.write(input);
        } catch (IOException e) {

        }
    }

    // When the activity is starting to get deleted from the stack(about to be terminated by whatever means)
    @Override
    public void onPause() {
        // Check if the activity is closing completely and not restarting
        if(isFinishing() == true) {
            // Try to stop the threads and close the in/out streams and socket
            try {
                // Set the thread stop conditions for every thread
                BluetoothVariables.stopReceiveThread = true;
                BluetoothVariables.stopContinuousThread = true;

                // Close in/out streams then the socket(order is important)
                BluetoothVariables.inStream.close();
                BluetoothVariables.outStream.close();
                BluetoothVariables.btSocket.close();
            } catch (IOException e) {
                // This part only exists because the methods call for it
            }
        }

        super.onPause();
    }

    // If the activity is restarting and not closing completely save some data
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the log text
        outState.putString("Saved Log", logText.getText().toString());

        super.onSaveInstanceState(outState);
    }

}// End of the send receive activity class!
