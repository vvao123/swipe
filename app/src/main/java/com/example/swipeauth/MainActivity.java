package com.example.swipeauth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.VibrationEffect;
import android.os.Vibrator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import com.example.swipeauth.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

// bluetooth part
    private static final String TAG = "MainActivity";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Same as in your Bluetooth device documentation
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice mBluetoothDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String deviceAddress = "00:00:00:00:00:00"; // Replace with your device's address
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 1;
    private Handler handler;
//
    // Data storage
    static final int DATA_COUNT = 80;
    static final int DATA_ENTRIES = 100000;
    String[] actions = new String[DATA_ENTRIES];
    int[] touchIndices = new int[DATA_ENTRIES];
    String[] directions = new String[DATA_ENTRIES];
    Long[] durations = new Long[DATA_ENTRIES];

    List<Long> timeStamp = new ArrayList<>();
    List<Long> timeStampEachTouch = new ArrayList<>(); // timeStamp but clear every action_up
    List<Double> pressures = new ArrayList<>();
    //for 3 finger
    List<Double> pressures1 = new ArrayList<>();
    List<Double> pressures2 = new ArrayList<>();
    List<Double> pressures3 = new ArrayList<>();
    List<Double> fingerSizes1 = new ArrayList<>();
    List<Double> fingerSizes2 = new ArrayList<>();
    List<Double> fingerSizes3 = new ArrayList<>();
    List<Double> fingerSizes = new ArrayList<>();
    List<Integer> coordX = new ArrayList<>();
    List<Integer> coordY = new ArrayList<>();

    List<Double> velocity = new ArrayList<>();

    //finger_distinguish
    Double fingerpressuremax1=0.0;
    Double fingerpressuremax2=0.0;
    Double fingerpressuremax3=0.0;
    Double fingerpressurebase1=0.0;
    Double fingerpressurebase2=0.0;
    Double fingerpressurebase3=0.0;
    boolean isnormal=true;  //only true apply squeeze binary input
    int debugbiint=0;
    int changetoregister=0; // this used to change to register mode later at action_up, but detection at action_move, so I first use it as signal

    double lastpressure=0.0;
    List<Integer> inputEachTouch= new ArrayList<>();
    Map<Integer, Long> inputEachTouchmap = new HashMap<>();
    int lastbioutput=-1;  //record last output number to calculate each number time
    boolean is3finger=false; //variable to record if this action_move is 3 finger or not




    int dataCount = 0;
    int moveIndex = 0;
    int touchIndex = 0;

    // Times
    long start;
    long end;

    // Coords
    int startX = 0;
    int startY = 0;
    int endX = 0;
    int endY = 0;


    boolean isauthen=false;
    String readMessage;



    private TextView swipeText;
    private Switch switchbutton;
    private Button button;



    private EditText usernameEditText;
    private EditText inputEditText;

    private VelocityTracker mVelocityTracker = null;
    private GestureDetector mGestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // doubletap删除最后一个字符
                System.out.println("Double tap detected with multiple pointers!");
                String currentText = inputEditText.getText().toString();
                if(currentText.length() > 0) {  // 检查是否为空字符串
                    String newText = currentText.substring(0, currentText.length() - 1); // 删除最后一个字符
                    inputEditText.setText(newText);
                    sendData(newText);
                }
                return true;


            }
//            @Override
//            public boolean onSingleTapConfirmed(MotionEvent e){
////                inputEditText.setText("aaa");
//                return true;
//            }
        });

        // acquire permission to save
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        // acquire permission to vibrate
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE}, 0);
        }
        // Create and start the accept thread.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
        } else {
            acceptThread = new AcceptThread(this);
            acceptThread.start();
        }
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:  // 对应ConnectedThread中MESSAGE_READ
                        byte[] readBuf = (byte[]) msg.obj;

                        // construct a string from the valid bytes in the buffer
                        readMessage = new String(readBuf, 0, msg.arg1);
                        VibrationEffect vibrationEffect = VibrationEffect.createOneShot(2000, 10);
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        // Vibrate for 2000 milliseconds
                        v.vibrate(vibrationEffect);
                        break;
                    case 1:  // 对应ConnectedThread中MESSAGE_READ
                        System.out.println("I am in case 1");
//                        byte[] readBuf1 = (byte[]) msg.obj;
//
//                        // construct a string from the valid bytes in the buffer
////                        readMessage = new String(readBuf1, 0, msg.arg1);
//                        text1.setText(readMessage);
//                        authenticate();


                        break;
                    // ... 可以添加更多的case来处理其他类型的消息 ...
                }
            }
        };
//


        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View mContent = binding.main;
        swipeText = binding.swipe;
        usernameEditText=binding.useidtext;
        inputEditText=binding.input;
//        inputEditText.setEnabled(false); //prevent user edit
        inputEditText.setKeyListener(null);
//        inputEditText.setText("123456");
        switchbutton=binding.switch3;
        button=binding.button;

        button.setVisibility(View.INVISIBLE); // or View.GONE
        switchbutton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Switch is in 'on' state, perform the necessary action
                    Toast.makeText(MainActivity.this, "change to register mode", Toast.LENGTH_SHORT).show();
                    switchbutton.setText("Register");
                    swipeText.setText("Please squeeze twice\n lightly the first time then hard");
                    isnormal=false;
                    rest();


                } else {
                    // Switch is in 'off' state, perform the necessary action
                    switchbutton.setText("Normal");
                    isnormal=true;
                    rest();
                }
            }
        });
//                usernameEditText = findViewById(R.id.useidtext);
//exclude  edgeswipe back gesture
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            mContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                @Override
//                public void onGlobalLayout() {
//                    mContent.requestFocus()
//                    mContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                    List<Rect> exclusionRects = new ArrayList<>();
//                    exclusionRects.add(new Rect(0, 0, mContent.getWidth(), mContent.getHeight()));
//                    System.out.println("!!!");
//                    System.out.println(mContent.getWidth());
//                    mContent.setSystemGestureExclusionRects(exclusionRects);
//                }
//            });
//        }


        // test embed python
//        if (!Python.isStarted()){
//            Python.start(new AndroidPlatform(this));
//        }
//        Python python=Python.getInstance();
//        PyObject pyObject=python.getModule("testoutput");
//        pyObject.callAttr("sayHello");

//
        inputEachTouchmap.put(0, 0L);
        inputEachTouchmap.put(1, 0L);
        inputEachTouchmap.put(2, 0L);
        inputEachTouchmap.put(3, 0L);
        inputEachTouchmap.put(4, 0L);
        inputEachTouchmap.put(5, 0L);
        inputEachTouchmap.put(6, 0L);
        inputEachTouchmap.put(7, 0L);


        mContent.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Let GestureDetector inspect all events.
                mGestureDetector.onTouchEvent(motionEvent);
                // Variables for velocity tracking
                int index = motionEvent.getActionIndex();
                int pointerId = motionEvent.getPointerId(index);
                is2FingerDoubleClick(motionEvent);
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        is3finger=false;
//                        dataCount++; // different with other count and index in action_up because it need to be reset to 0 during action_move
//                        if(switchbutton.isChecked()&&dataCount == 2) {
//                            swipeText.setText("Finished registration\nPlease input");
//                            break;
//                        }

                        // Action check
//                        view.performClick();
                        System.out.println("Down");
                        actions[moveIndex] = "Down";

                        // Start time
                        start = System.currentTimeMillis();
                        System.out.println(start);

                        // Coords
                        startX = (int) motionEvent.getX();
                        startY = (int) motionEvent.getY();
                        // Velocity tracking
//                        if(mVelocityTracker == null) {
//                            // Retrieve a new VelocityTracker object to watch the
//                            // velocity of a motion.
//                            mVelocityTracker = VelocityTracker.obtain();
//                        }
//                        else {
//                            // Reset the velocity tracker back to its initial state.
//                            mVelocityTracker.clear();
//                        }
//                        // Add a user's movement to the tracker.
//                        mVelocityTracker.addMovement(motionEvent);

                        // Count
//                        touchIndices[moveIndex] = touchIndex;
//                        moveIndex++;
                        break;

                    // Move
                    case MotionEvent.ACTION_MOVE:
//                        if(dataCount == DATA_COUNT) {
////                            swipeText.setText("Finished collecting\nPlease export data");
////                            break;
//                        }
//                        else
////                            swipeText.setText("Swiping...");

                        samples(motionEvent);   //**important

                        // Action check
                        System.out.println("Move");


                        // End time for each interval
//                        end = System.currentTimeMillis();
//                        durations[moveIndex] = end - start;
                        // New start time
//                        start = System.currentTimeMillis();

//                        // Finger size
//                        fingerSizes[moveIndex] = motionEvent.getSize();
//
//                        // Pressure
//                        pressures[moveIndex] = motionEvent.getPressure();

                        // Velocity tracking
                        // mVelocityTracker.addMovement(motionEvent);
                        // When you want to determine the velocity, call
                        // computeCurrentVelocity(). Then call getXVelocity()
                        // and getYVelocity() to retrieve the velocity for each pointer ID.
                        // mVelocityTracker.computeCurrentVelocity(1);
                        // Log velocity of pixels per second
                        // Best practice to use VelocityTrackerCompat where possible.

                        break;

                    case MotionEvent.ACTION_UP:
                        // Time
                        end = System.currentTimeMillis();
//                        text1.setText("please squeeze");
//                        swipeText.setText("please squeeze");
                        debugbiint=0;

                        durations[touchIndex] = (end - start);
                        System.out.println("durations");
                        System.out.println(durations[touchIndex]);

                        System.out.println(start);

//                        System.out.println(velocity.size());
                        System.out.println(pressures.size());
                        System.out.println(coordX.size());
                        System.out.println(coordX.size());
                        System.out.println(moveIndex);
                        System.out.println(touchIndex);


                        // Action check
                        System.out.println("Up");
                        actions[moveIndex] = "UP";
                        // Count
//                        touchIndices[moveIndex] = touchIndex;
//                        moveIndex++;
                        touchIndex++;
                        dataCount++;

                        // Coords
                        endX = (int) motionEvent.getX();
                        endY = (int) motionEvent.getY();
//                        System.out.println("datacount is here::::");
//                        System.out.println(dataCount);

                        //** code of registration 3fingerBinaryPassword.**//

                        if(switchbutton.isChecked()&&dataCount == 2) {
                            swipeText.setText("Finished registration\nPlease input");
                            train();
                            switchbutton.setChecked(false);

                            break;
                        }
                        else if(switchbutton.isChecked()&&dataCount == 1){
                            swipeText.setText("Good,first squeeze complete!\nnow please squeeze hard");
                        }
                        else if(changetoregister==1){
                            switchbutton.setChecked(true);//non exist user change to register mode automatically
                            changetoregister=0;
                        }
                        else if(is3finger==true){ //normal 3finger input
                            //type1.decide input number with mode
//                        inputEditText.setText(inputEditText.getText()+Integer.toString(findMode(inputEachTouch)));
//                        inputEachTouch.clear();
                            //type2.decide input number with longest time  add a special rule only when 0 be pressed, 0 can be as output
                            boolean isonly0=true;
                            Map.Entry<Integer, Long> maxEntry = null;
                            for (Map.Entry<Integer, Long> entry : inputEachTouchmap.entrySet()) {
                                if(entry.getKey()!=0&&entry.getValue()>0){
                                    //if there is other number, then 0 willnot be output
                                    isonly0=false;
                                    inputEachTouchmap.put(0, 0L);
                                    break;
                                }
                            }
                            for (Map.Entry<Integer, Long> entry : inputEachTouchmap.entrySet()) {

                                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                                    maxEntry = entry;
                                }
                            }
                            sendData(inputEditText.getText()+Integer.toString( maxEntry.getKey()));
                            inputEditText.setText(inputEditText.getText()+Integer.toString( maxEntry.getKey()));

                            for (Integer key : inputEachTouchmap.keySet()) {//fresh dictionary
                                inputEachTouchmap.put(key, 0L);
                            }
                            lastbioutput=-1;
                            timeStampEachTouch.clear();
                            swipeText.setText("Please squeeze");

                        }
//                        Direction direction = getDirection(startX, startY, endX, endY);


//                        if (direction == Direction.down_up){
//                            c1++;
//                            text1.setText("to top " + c1);
//                        }
//                        else if (direction == Direction.bottomLeft_topRight) {
//                            c2++;
//                            text2.setText("to top right " + c2);
//                        }
//                        else if (direction == Direction.left_right) {
//                            c3++;
//                            text3.setText("to right " + c3);
//                        }
//                        else if (direction == Direction.topLeft_bottomRight) {
//                            c4++;
//                            text4.setText("to bottom right " + c4);
//                        }
//                        else if (direction == Direction.up_down) {
//                            c5++;
//                            text5.setText("to bottom " + c5);
//                        }
//                        else if (direction == Direction.topRight_bottomLeft) {
//                            c6++;
//                            text6.setText("to bottom left " + c6);
//                        }
//                        else if (direction == Direction.right_left) {
//                            c7++;
//                            text7.setText("to left " + c7);
//                        }
//                        else if (direction == Direction.bottomRight_topLeft) {
//                            c8++;
//                            text8.setText("to top left " + c8);
//                        }

//                        // Finger size
//                        fingerSizes[moveIndex] = motionEvent.getSize();
//
//                        // Pressure
//                        pressures[moveIndex] = motionEvent.getPressure();


////                        used in  pressurebar
//                        System.out.println("before loop");
//                        System.out.println(readMessage);
//                        readMessage="tmp";
//                        while(true){
//                            sendData("0.00\n");
//                            System.out.println(readMessage);
//                            if(readMessage!=null &&readMessage.equals("0.00")){
//                                break;
//                            }
//
//                        }
//                        readMessage="tmp";
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        // Return a VelocityTracker object back to be re-used by others.
//                        mVelocityTracker.recycle();
//                        sendData("0.0\n");
                        break;
                    default:
//                        sendData("0.0\n");
                        break;
                }
                return true;
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure to cleanup when the activity is destroyed.
        if (acceptThread != null) {
            acceptThread.cancel();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                acceptThread = new AcceptThread(this); //create a new bt thread
                acceptThread.start();
            } else {
                // Permission denied. Handle appropriately.
            }
        }
    }
    private void sendMessage(String message) {
        if (outputStream != null) {
            try {
                outputStream.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class AcceptThread extends Thread {
        private Context context;
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(Context context) { //先接受连接再传读东西
            this.context = context;
            BluetoothServerSocket tmp = null;
            try {
                // Use a well-known UUID for this application.
                UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

//                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return TODO;
//                }
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("My App", MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    manageMyConnectedSocket(socket); //start a connected thread
//                    new ConnectedThread(socket).start();
//                    Message msg = handler.obtainMessage(1, socket);
//                    handler.sendMessage(msg);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close the server socket", e);
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
                tmpIn.available();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    String receivedString=new String(mmBuffer, 0, numBytes);
                    System.out.println(receivedString);
                    int tag=-1;
                    if (receivedString.equals("doauth")) {
                        tag = 1;
                    } else if(receivedString.equals("ready")) {
                        tag = 0;  //“ready”
                    }

                    Message readMsg = handler.obtainMessage(tag, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        100, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        handler.obtainMessage(2);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }



        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

//    // Calculate swipe angles
//    public Direction getDirection(float x1, float y1, float x2, float y2){
//        double rad = Math.atan2(y1-y2,x2-x1) + Math.PI;
//        double angle = (rad*180/Math.PI + 180)%360;
//        return Direction.fromAngle(angle);
//    }
//
//    public enum Direction{
//        up_down,
//        topRight_bottomLeft,
//        right_left,
//        bottomRight_topLeft,
//        down_up,
//        bottomLeft_topRight,
//        left_right,
//        topLeft_bottomRight;
//
//        public static Direction fromAngle(double angle){
//            if (inRange(angle, 75, 105)){
//                return Direction.down_up;
//            }
//            else if (inRange(angle, 15, 75)){
//                return Direction.bottomLeft_topRight;
//            }
//            else if (inRange(angle, 0, 15) || inRange(angle, 345, 360)){
//                return Direction.left_right;
//            }
//            else if (inRange(angle, 285, 345)){
//                return Direction.topLeft_bottomRight;
//            }
//            else if (inRange(angle, 255, 285)){
//                return Direction.up_down;
//            }
//            else if (inRange(angle, 195, 255)){
//                return Direction.topRight_bottomLeft;
//            }
//            else if (inRange(angle, 165, 195)){
//                return Direction.right_left;
//            }
//            else {
//                return  Direction.bottomRight_topLeft;
//            }
//        }
//
//        private static boolean inRange(double angle, float init, float end){
//            return (angle >= init) && (angle < end);
//        }
//    }

    // Check if all directions have enough number of swipes collected
//    public boolean validation (int c1, int c2, int c3, int c4, int c5, int c6, int c7, int c8){
//        int[] array = new int[]{c1, c2, c3, c4, c5, c6 ,c7, c8};
//        boolean v = false;
//        for (int j : array) {
//            v = j >= 10;
//        }
//        return v;
//    }

    // Access the batched historical event data points
    public void samples(MotionEvent ev) {
        double curtotalpressure;
        int curtotalX;
        int curtotalY;
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();
        System.out.printf("historySize:%d",historySize);
        System.out.printf("pointerCount:%d",pointerCount);
//        mVelocityTracker.addMovement(ev);
//        for (int h = 0; h < historySize; h++) {
////            System.out.printf("At time %d:", ev.getHistoricalEventTime(h));
//                curtotalpressure=0;
//            for (int p = 0; p < pointerCount; p++) {
////                System.out.printf("  pressure: (%f)|  ",
////                        ev.getHistoricalPressure(p, h));
//                pressures.add((double) ev.getHistoricalPressure(p, h));
//                curtotalpressure+=(double) ev.getHistoricalPressure(p, h);
//                fingerSizes.add((double) ev.getHistoricalSize(p, h));
//                int curX=(int) ev.getHistoricalX(p, h);
//                int curY=(int) ev.getHistoricalY(p, h);
//                if(timeStamp.size()>0){
//                    //not the first point calculate velocity
//                    double distance= calculateDistance(getLastElement(coordX),curX,getLastElement(coordY),curY);
//                    double vel=distance/(ev.getHistoricalEventTime(h)-getLastElement(timeStamp));
//                    velocity.add(vel);
//                }
//
//                timeStamp.add(ev.getHistoricalEventTime(h));
//                coordX.add(curX);
//                coordY.add(curY);
//                actions[moveIndex] = "Move";
//                touchIndices[moveIndex] = touchIndex;
//                moveIndex++;
//
//            }
////            sendData(String.valueOf(curtotalpressure)+"\n");
//        }
        System.out.printf("At time %d:", ev.getEventTime());
        curtotalpressure=0;
        curtotalX=0;
        curtotalY=0;
        if(pointerCount<3)
            return;
        is3finger=true;
        int minY = 9999999;
        int minYindex = -1;

        for (int p = 0; p < pointerCount; p++) {
            if(minY>(int) ev.getY(p)){
                minY=(int)ev.getY(p);
                minYindex=p;
            }
//            System.out.printf("  pressure: (%f)|  ", ev.getPressure(p));
//            pressures.add((double) ev.getPressure(p));
            curtotalpressure+=(double) ev.getPressure(p);
            curtotalX+=(int) ev.getX(p);
            curtotalY+=(int) ev.getY(p);
//            fingerSizes.add((double) ev.getSize(p));
//            double distance= calculateDistance(getLastElement(coordX),(int) ev.getX(p),getLastElement(coordY),(int) ev.getY(p));
//            double vel=distance/(ev.getEventTime()-getLastElement(timeStamp));
//            velocity.add(vel);

//            coordX.add((int) ev.getX(p));
//            coordY.add((int) ev.getY(p));


//            if((int) ev.getX(p)>500){
//                System.out.println("right");
//            }
//            else{
//                System.out.println("left");
//            }

        }
        timeStamp.add(ev.getEventTime());
        timeStampEachTouch.add(ev.getEventTime());
        pressures.add(curtotalpressure);
        coordX.add(curtotalX-3*(int) ev.getX(minYindex));
        coordY.add(curtotalY-3*(int) ev.getY(minYindex));
        List<Integer> listtmp = new ArrayList<>(Arrays.asList(0, 1, 2));
        System.out.println(listtmp+","+minYindex);
        listtmp.remove(minYindex);  // remove smallest one and compare the rest two

        if(!isnormal){   //不是输入所以是register
            if((int) ev.getY(listtmp.get(0))>(int) ev.getY(listtmp.get(1))){
                pressures1.add((double) ev.getPressure(listtmp.get(0)));
                pressures2.add((double) ev.getPressure(listtmp.get(1)));
                fingerSizes1.add((double) ev.getSize(listtmp.get(0)));
                fingerSizes2.add((double) ev.getSize(listtmp.get(1)));
            }
            else{
                pressures1.add((double) ev.getPressure(listtmp.get(1)));
                pressures2.add((double) ev.getPressure(listtmp.get(0)));
                fingerSizes1.add((double) ev.getSize(listtmp.get(1)));
                fingerSizes2.add((double) ev.getSize(listtmp.get(0)));

            }
            pressures3.add((double) ev.getPressure(minYindex));
            fingerSizes3.add((double) ev.getSize(minYindex));

        }
        else{//output binary code
            if(!
                    readCsvFile(getFilesDir()+"/"+usernameEditText.getText().toString()+"pressmaxbase.csv")){
                //user not exist
                changetoregister=1;
                swipeText.setText("This user doesn't exist, register first");
//                switchbutton.setChecked(true);
            }
            else{  // open user csv successfully
                int bioutput=0;
                if((int) ev.getY(listtmp.get(0))>(int) ev.getY(listtmp.get(1))){
                    System.out.println("112121");


                    if((double) ev.getPressure(listtmp.get(0))>(fingerpressuremax1+fingerpressurebase1)/2){
                        bioutput+=1;
                    }
                    if((double) ev.getPressure(listtmp.get(1))>(fingerpressuremax2+fingerpressurebase2)/2){
                        bioutput+=2;
                    }
                }
                else{

                    if((double) ev.getPressure(listtmp.get(0))>(fingerpressuremax2+fingerpressurebase2)/2){
                        bioutput+=2;
                    }
                    if((double) ev.getPressure(listtmp.get(1))>(fingerpressuremax1+fingerpressurebase1)/2){
                        bioutput+=1;
                    }

                }
                if((double) ev.getPressure(minYindex)>((fingerpressuremax3+fingerpressurebase3)/2)){
                    bioutput+=4;
                }
                swipeText.setText(Integer.toString(bioutput));

                sendData(inputEditText.getText()+"<color=#ff0000>"+Integer.toString(bioutput)+"</color>");
                if(timeStampEachTouch.size()>1){
                    inputEachTouchmap.put(lastbioutput,inputEachTouchmap.get(lastbioutput)+timeStampEachTouch.get(timeStampEachTouch.size()-1)-timeStampEachTouch.get(timeStampEachTouch.size()-2)); //current-last(倒数第一减倒数第二)
                }
                lastbioutput=bioutput;
//                inputEachTouch.add(bioutput);


            }

//            if( bioutput>debugbiint){
//                debugbiint=bioutput;

//                text1.setText(Integer.toString(bioutput));

//            }


        }



//        sendData(String.valueOf(curtotalpressure)+"\n"); //pressure bar function
        actions[moveIndex] = "Move";
        touchIndices[moveIndex] = touchIndex;
        moveIndex++;
    }

    public static <E> E getLastElement(List<E> list)
    {
        int lastIdx = list.size() - 1;
        E lastElement = list.get(lastIdx);
        return lastElement;
    }
    public double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public  void send100(View view){  //test use only
        int n;
        for(n=0;n<100;n++){
            sendData(Integer.toString(n));
        }
    }

    public void rest(){
        System.out.println("datacount in rest::::");

        // reset
        dataCount = 0;
        System.out.println(dataCount);
        moveIndex = 0;
        touchIndex = 0;
        actions = new String[DATA_ENTRIES];
        touchIndices = new int[DATA_ENTRIES];
        directions = new String[DATA_ENTRIES];
        durations = new Long[DATA_ENTRIES];

        timeStamp.clear();
        timeStampEachTouch.clear();
        pressures.clear();
        fingerSizes.clear();
        coordX.clear();
        coordY.clear();
        pressures1.clear();
        pressures3.clear();
        pressures2.clear();
        for (Integer key : inputEachTouchmap.keySet()) {
            inputEachTouchmap.put(key, 0L);
        }

    }
    public void fresh(View view){
        inputEditText.setText("");
        swipeText.setText("Please squeeze");
        rest();
    }
    public void test(View view){
        VibrationEffect vibrationEffect = VibrationEffect.createOneShot(2000, 10);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(vibrationEffect);
    }

    public void train(View view){
        train();
    }
    public void train() { //acquire max and min limit and threshold
        VibrationEffect vibrationEffect = VibrationEffect.createOneShot(2000, 30);
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(vibrationEffect);

//        String username = usernameEditText.getText().toString();
//        if (!Python.isStarted()){
//            Python.start(new AndroidPlatform(this));
//        }
//        Python python=Python.getInstance();
//        PyObject pyObject=python.getModule("authpy");
//        pyObject.callAttr("train",username);
//        rest();

        int count=0;
        int i = touchIndices[count];
        double totalbase1=0;
        double totalbase2=0;
        double totalbase3=0;
        double totalmax1=0;
        double totalmax2=0;
        double totalmax3=0;
        int stage1count=0;
        int stage2count=0;
//        double [] arrbase1=new double[100];
//        double [] arrbase2=new double[100];
//        double [] arrbase3=new double[100];
//        double [] arrmax1=new double[100];
//        double [] arrmax2=new double[100];
//        double [] arrmax3=new double[100];
        List<Double> listbase1 = new ArrayList<>();
        List<Double> listbase2 = new ArrayList<>();
        List<Double> listbase3 = new ArrayList<>();
        List<Double> listmax1 = new ArrayList<>();
        List<Double> listmax2 = new ArrayList<>();
        List<Double> listmax3 = new ArrayList<>();

        boolean stage1=true;// 第一个动作算轻放，第二个算最重压
        while(true) {
            int temp = touchIndices[count];
            if (stage1) {
                totalbase1+=pressures1.get(count);
                totalbase2+=pressures2.get(count);
                totalbase3+=pressures3.get(count);
                listbase1.add(pressures1.get(count));
                listbase2.add(pressures2.get(count));
                listbase3.add(pressures3.get(count));
            }
            else{
                totalmax1+=pressures1.get(count);
                totalmax2+=pressures2.get(count);
                totalmax3+=pressures3.get(count);
                listmax1.add(pressures1.get(count));
                listmax2.add(pressures2.get(count));
                listmax3.add(pressures3.get(count));
            }
            count++;
            if(count>=pressures.size())
                break;
            if(touchIndices[count] != temp){
                i = touchIndices[count];
                stage1=false;
                stage1count=count;
            }
        };
//        double fingerpressurebase11;
//        double fingerpressurebase21;
//        double fingerpressurebase31;
//        double fingerpressuremax11;
//        double fingerpressuremax21;
//        double fingerpressuremax31;
        stage2count=count-stage1count;
//        fingerpressurebase1= totalbase1/stage1count;
//        fingerpressurebase2= totalbase2/stage1count;
//        fingerpressurebase3= totalbase3/stage1count;
//        fingerpressuremax1= totalmax1/stage2count;
//        fingerpressuremax2= totalmax2/stage2count;
//        fingerpressuremax3= totalmax3/stage2count;
        fingerpressurebase1= findMedian(listbase1);
        fingerpressurebase2= findMedian(listbase2);
        fingerpressurebase3= findMedian(listbase3);
        fingerpressuremax1= findMedian(listmax1);
        fingerpressuremax2= findMedian(listmax2);
        fingerpressuremax3= findMedian(listmax3);


//
//        System.out.println(fingerpressurebase1+","+fingerpressurebase11);
//        System.out.println(fingerpressurebase2+","+fingerpressurebase21);
//        System.out.println(fingerpressurebase3+","+fingerpressurebase31);
//        System.out.println(fingerpressuremax1+","+fingerpressuremax11);
//        System.out.println(fingerpressuremax2+","+fingerpressuremax21);
//        System.out.println(fingerpressuremax3+","+fingerpressuremax31);

        rest();

        String username = usernameEditText.getText().toString();
        StringBuilder data = new StringBuilder();
        data.append(fingerpressurebase1).append(",").append( fingerpressuremax1).append("\n");
        data.append(fingerpressurebase2).append(",").append( fingerpressuremax2).append("\n");
        data.append(fingerpressurebase3).append(",").append( fingerpressuremax3).append("\n");

        try {

//            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "training.csv");
            File file = new File(getFilesDir(), username+"pressmaxbase.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(data.toString().getBytes());
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public void authenticate(){
        System.out.println(pressures.size());
        if(pressures.size()==0){
            System.out.println("gogogo");
            return;

        }

        String username = usernameEditText.getText().toString();
        StringBuilder data = new StringBuilder();
        int count = 0;
        int i = touchIndices[count];
//        data.append(directions[i]).append(",");
        data.append(durations[i]).append(",");
        while(true) {
            int temp = touchIndices[count];
            data.append(pressures.get(count)).append(",").
//                    append(coordX.get(count4press)).append(",").
        append(coordY.get(count)).append(",");
            count++;
            if(count>=pressures.size())
                break;
            if(touchIndices[count] != temp){

                i = touchIndices[count];
                data.append("\n").append(durations[i]).append(",");
            }
        };

        try {
//            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "training.csv");
            File file = new File(getFilesDir(), username+"authen.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(data.toString().getBytes());
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        rest();

        if (!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        Python python=Python.getInstance();
        PyObject pyObject=python.getModule("doauth");
        PyObject result=pyObject.callAttr("doau",username);
        System.out.println(result.toString());
        if( result.toString().equals("[1]")){
            swipeText.setText("success");
            sendData("success\n");

        }
        else{
            swipeText.setText("denied");
            sendData("denied\n");
            rest();

        }

    }
    public void authenticate(View view) {
        authenticate();

    }
    public void sendData(String data) {
        String data1=data+"\n";
        if (connectedThread != null) {
            connectedThread.write(data1.getBytes());
        }
    }
    private void manageMyConnectedSocket(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }
    public void export(View view) {
        System.out.println(Arrays.toString(touchIndices));
        System.out.println(Arrays.toString(durations));

        String username = usernameEditText.getText().toString();
        StringBuilder data = new StringBuilder();
        int count = 0;
        int i = touchIndices[count];
//        data.append(directions[i]).append(",");
        data.append(durations[i]).append(",");

        while(true) {

            int temp = touchIndices[count];
            data.append(pressures.get(count)).append(",").
//                    append(coordX.get(count4press)).append(",").
            append(coordY.get(count)).append(",");
            count++;
            if(count>=pressures.size())
                break;
            if(touchIndices[count] != temp){

                i = touchIndices[count];
                data.append("\n").append(durations[i]).append(",");
            }
        };

        try {

//            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "training.csv");
            File file = new File(getFilesDir(), username+"training.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(data.toString().getBytes());
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean readCsvFile(String path) {
        String csvLine;
        File file = new File(path);
        System.out.println(path);
        // If the file does not exist, return false
        if (!file.exists()) {
            return false;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String[] maxarrr=new String[3];
            String[] basearrr=new String[3];
            int n=0;
            while ((csvLine = br.readLine()) != null) {
                String[] dataarr = csvLine.split(",");
                // 'data' array contains your csv data.
                maxarrr[n]=dataarr[1];
                basearrr[n]=dataarr[0];
                n++;
            }
            fingerpressuremax1=Double.valueOf(maxarrr[0]);
            fingerpressuremax2=Double.valueOf(maxarrr[1]);
            fingerpressuremax3=Double.valueOf(maxarrr[2]);
            fingerpressurebase1=Double.valueOf(basearrr[0]);
            fingerpressurebase2=Double.valueOf(basearrr[1]);
            fingerpressurebase3=Double.valueOf(basearrr[2]);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading CSV file: " + ex);
        }

        return true;
    }
    public double findMedian(List<Double> nums) {
        Collections.sort(nums);
        int n = nums.size();
        return (n % 2 != 0) ? nums.get(n / 2) : (nums.get((n - 1) / 2) + nums.get(n / 2)) / 2.0;
    }
    private static final int TIMEOUT = ViewConfiguration.getDoubleTapTimeout() + 100;
    private long mFirstDownTime = 0;
    private boolean mSeparateTouches = false;
    private byte mTwoFingerTapCount = 0;
    private void is2FingerDoubleClick(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mFirstDownTime == 0 || event.getEventTime() - mFirstDownTime > TIMEOUT)
                    twofingerreset(event.getDownTime());
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 2)
                    mTwoFingerTapCount++;
                else
                    mFirstDownTime = 0;
                break;
            case MotionEvent.ACTION_UP:
                if (!mSeparateTouches)
                    mSeparateTouches = true;
                else if (mTwoFingerTapCount == 2 && event.getEventTime() - mFirstDownTime < TIMEOUT) {
                    mFirstDownTime = 0;
                    Toast.makeText(MainActivity.this, "双指双击事件", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void twofingerreset(long time) {
        mFirstDownTime = time;
        mSeparateTouches = false;
        mTwoFingerTapCount = 0;
    }
//    public static int findMode(List<Integer> list) {
//        Map<Integer, Integer> countMap = new HashMap<>();
//        // 计算每个元素出现的次数
//        for (Integer num : list) {
//            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
//        }
//
//        List<Integer> modes = new ArrayList<>();
//        int maxCount = 0;
//        // 遍历map，找出出现次数最多的元素
//        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
//            int count = entry.getValue();
//            if (count > maxCount) {
//                modes.clear();
//                modes.add(entry.getKey());
//                maxCount = count;
//            } else if (count == maxCount) {
//                modes.add(entry.getKey());
//            }
//        }
//        return modes.get(0);
//    }
}