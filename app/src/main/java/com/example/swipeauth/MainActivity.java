package com.example.swipeauth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowInsetsCompat;

import android.os.VibrationEffect;
import android.os.Vibrator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import com.example.swipeauth.databinding.ActivityMainBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
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

    Double fingerpressuremax1=0.0;
    Double fingerpressuremax2=0.0;
    Double fingerpressuremax3=0.0;
    Double fingerpressurebase1=0.0;
    Double fingerpressurebase2=0.0;
    Double fingerpressurebase3=0.0;
    boolean pressure3mode=false;
    int debugbiint=0;
    int lasttimestamp=0;
    double lastpressure=0.0



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

    // Direction counts
    int c1;
    int c2;
    int c3;
    int c4;
    int c5;
    int c6;
    int c7;
    int c8;

    boolean isauthen=false;
    String readMessage;


//    List<String> directionLable = new ArrayList<>();

    private TextView swipeText;

    // Labels for each direction
    private TextView text1;
    private TextView text2;
    private TextView text3;
    private TextView text4;
    private TextView text5;
    private TextView text6;
    private TextView text7;
    private TextView text8;

    private EditText usernameEditText;

    private VelocityTracker mVelocityTracker = null;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                        text1.setText(readMessage);
                        VibrationEffect vibrationEffect = VibrationEffect.createOneShot(500, 128);
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        // Vibrate for 500 milliseconds
                        v.vibrate(vibrationEffect);
                        break;
                    case 1:  // 对应ConnectedThread中MESSAGE_READ
                        System.out.println("I am in case 1");
//                        byte[] readBuf1 = (byte[]) msg.obj;
//
//                        // construct a string from the valid bytes in the buffer
////                        readMessage = new String(readBuf1, 0, msg.arg1);
//                        text1.setText(readMessage);
                        authenticate();


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
        usernameEditText = findViewById(R.id.useidtext);
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

        text1 = binding.up;
        text2 = binding.topRight;
        text3 = binding.right;
        text4 = binding.bottomRight;
        text5 = binding.down;
        text6 = binding.bottomLeft;
        text7 = binding.left;
        text8 = binding.topLeft;

        mContent.setOnTouchListener(new View.OnTouchListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Variables for velocity tracking
                int index = motionEvent.getActionIndex();
                int pointerId = motionEvent.getPointerId(index);

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(dataCount == DATA_COUNT) {
//                            swipeText.setText("Finished collecting\nPlease export data");
                            break;
                        }

                        // Action check
                        view.performClick();
                        System.out.println("Down");
                        actions[moveIndex] = "Down";

                        // Start time
                        start = System.currentTimeMillis();
                        System.out.println(start);

                        // Coords
                        startX = (int) motionEvent.getX();
                        startY = (int) motionEvent.getY();



                        // Velocity tracking
                        if(mVelocityTracker == null) {
                            // Retrieve a new VelocityTracker object to watch the
                            // velocity of a motion.
                            mVelocityTracker = VelocityTracker.obtain();
                        }
                        else {
                            // Reset the velocity tracker back to its initial state.
                            mVelocityTracker.clear();
                        }
                        // Add a user's movement to the tracker.
                        mVelocityTracker.addMovement(motionEvent);

                        // Count
//                        touchIndices[moveIndex] = touchIndex;
//                        moveIndex++;


                        break;

                    // Move
                    case MotionEvent.ACTION_MOVE:
                        if(dataCount == DATA_COUNT) {
//                            swipeText.setText("Finished collecting\nPlease export data");
//                            break;
                        }
                        else
//                            swipeText.setText("Swiping...");

                        samples(motionEvent);

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
                        swipeText.setText("please squeeze");
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
                        if(false) {
//                            swipeText.setText("Finished collecting\nPlease export data");

                            break;
                        }
                        else {
                            dataCount++;
//                            swipeText.setText("Swipes: " + dataCount);
                        }

                        // Action check
                        System.out.println("Up");
                        actions[moveIndex] = "UP";
                        // Count
//                        touchIndices[moveIndex] = touchIndex;
//                        moveIndex++;
                        touchIndex++;



                        // Coords
                        endX = (int) motionEvent.getX();
                        endY = (int) motionEvent.getY();

                        Direction direction = getDirection(startX, startY, endX, endY);
                        directions[touchIndex] = direction.toString();

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
                        mVelocityTracker.recycle();
                        sendData("0.0\n");
                        break;
                    default:
                        sendData("0.0\n");
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
                acceptThread = new AcceptThread(this);
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

        public AcceptThread(Context context) {
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
                    manageMyConnectedSocket(socket);
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

    // Calculate swipe angles
    public Direction getDirection(float x1, float y1, float x2, float y2){
        double rad = Math.atan2(y1-y2,x2-x1) + Math.PI;
        double angle = (rad*180/Math.PI + 180)%360;
        return Direction.fromAngle(angle);
    }

    public enum Direction{
        up_down,
        topRight_bottomLeft,
        right_left,
        bottomRight_topLeft,
        down_up,
        bottomLeft_topRight,
        left_right,
        topLeft_bottomRight;

        public static Direction fromAngle(double angle){
            if (inRange(angle, 75, 105)){
                return Direction.down_up;
            }
            else if (inRange(angle, 15, 75)){
                return Direction.bottomLeft_topRight;
            }
            else if (inRange(angle, 0, 15) || inRange(angle, 345, 360)){
                return Direction.left_right;
            }
            else if (inRange(angle, 285, 345)){
                return Direction.topLeft_bottomRight;
            }
            else if (inRange(angle, 255, 285)){
                return Direction.up_down;
            }
            else if (inRange(angle, 195, 255)){
                return Direction.topRight_bottomLeft;
            }
            else if (inRange(angle, 165, 195)){
                return Direction.right_left;
            }
            else {
                return  Direction.bottomRight_topLeft;
            }
        }

        private static boolean inRange(double angle, float init, float end){
            return (angle >= init) && (angle < end);
        }
    }

    // Check if all directions have enough number of swipes collected
    public boolean validation (int c1, int c2, int c3, int c4, int c5, int c6, int c7, int c8){
        int[] array = new int[]{c1, c2, c3, c4, c5, c6 ,c7, c8};
        boolean v = false;
        for (int j : array) {
            v = j >= 10;
        }
        return v;
    }

    // Access the batched historical event data points
    public void samples(MotionEvent ev) {
        double curtotalpressure;
        int curtotalX;
        int curtotalY;
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();
        System.out.printf("historySize:%d",historySize);
        System.out.printf("pointerCount:%d",pointerCount);
        mVelocityTracker.addMovement(ev);
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
            timeStamp.add(ev.getEventTime());
//            coordX.add((int) ev.getX(p));
//            coordY.add((int) ev.getY(p));


//            if((int) ev.getX(p)>500){
//                System.out.println("right");
//            }
//            else{
//                System.out.println("left");
//            }

        }
        pressures.add(curtotalpressure);
        coordX.add(curtotalX-3*(int) ev.getX(minYindex));
        coordY.add(curtotalY-3*(int) ev.getY(minYindex));
        List<Integer> listtmp = new ArrayList<>(Arrays.asList(0, 1, 2));
        listtmp.remove(minYindex);  // remove smallest one and compare the rest two
        System.out.println(listtmp.size());
        if(!pressure3mode){
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
            int bioutput=0;
            if((int) ev.getY(listtmp.get(0))>(int) ev.getY(listtmp.get(1))){
                System.out.println("112121");

                if((double) ev.getPressure(listtmp.get(0))>fingerpressuremax1*0.9){
                    bioutput+=1;
                }
                if((double) ev.getPressure(listtmp.get(1))>fingerpressuremax2*0.9){
                    bioutput+=2;
                }
            }
            else{

                if((double) ev.getPressure(listtmp.get(0))>fingerpressuremax2*0.9){
                    bioutput+=2;
                }
                if((double) ev.getPressure(listtmp.get(1))>fingerpressuremax1*0.9){
                    bioutput+=1;
                }

            }
            if((double) ev.getPressure(minYindex)>(fingerpressuremax3*0.9)){
                bioutput+=4;
            }
//            if( bioutput>debugbiint){
//                debugbiint=bioutput;

//                text1.setText(Integer.toString(bioutput));
                swipeText.setText(Integer.toString(bioutput));
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
    public void rest(){
        // reset
        dataCount = 0;
        moveIndex = 0;
        touchIndex = 0;
        actions = new String[DATA_ENTRIES];
        touchIndices = new int[DATA_ENTRIES];
        directions = new String[DATA_ENTRIES];
        durations = new Long[DATA_ENTRIES];

        timeStamp.clear();
        pressures.clear();
        fingerSizes.clear();
        coordX.clear();
        coordY.clear();
    }


    public void train(View view) {
        VibrationEffect vibrationEffect = VibrationEffect.createOneShot(500, 30);
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
        boolean stage1=true;// 第一个动作算轻放，第二个算最重压
        while(true) {

            int temp = touchIndices[count];
            if (stage1) {
                totalbase1+=pressures1.get(count);
                totalbase2+=pressures2.get(count);
                totalbase3+=pressures3.get(count);
            }
            else{
                totalmax1+=pressures1.get(count);
                totalmax2+=pressures2.get(count);
                totalmax3+=pressures3.get(count);
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
        stage2count=count-stage1count;
        fingerpressurebase1= totalbase1/stage1count;
        fingerpressurebase2= totalbase2/stage1count;
        fingerpressurebase3= totalbase3/stage1count;
        fingerpressuremax1= totalmax1/stage2count;
        fingerpressuremax2= totalmax2/stage2count;
        fingerpressuremax3= totalmax3/stage2count;
        System.out.println(fingerpressurebase1);
        System.out.println(fingerpressurebase1);
        System.out.println(fingerpressurebase1);
        System.out.println(fingerpressuremax1);
        System.out.println(fingerpressuremax2);
        System.out.println(fingerpressuremax3);

        pressure3mode=true;
        rest();


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
        if (connectedThread != null) {
            connectedThread.write(data.getBytes());
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
}