package com.example.edubot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener  {

    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    private TextView userChat;
    private TextView aiChat;
    private  String question="";
    private  int x;

    //For Bluetooth
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int USER = 10001;
    private static final int BOT = 10002;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private String uuid = UUID.randomUUID().toString();
    private String deviceName = null;
    private String deviceAddress;
    private int i=0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //database
        DatabaseReference dbQuestions = FirebaseDatabase.getInstance().getReference().child("Questions");

        //permission

        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //XML connect

        userChat = findViewById(R.id.userChat);
        aiChat = findViewById(R.id.aiChat);



        //Bot Blutooth
        final ImageView buttonConnect = findViewById(R.id.blutoothButton);

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            aiChat.setText("Connecting to " + deviceName + "...");

            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new MainActivity.CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();

        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handleMessage: " + msg.toString());
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                aiChat.setText("Connected");
                                Log.d(TAG, "handleMessage: Connected");
                                buttonConnect.setEnabled(true);
                                break;
                            case -1:
                                buttonConnect.setEnabled(true);
                                Log.d(TAG, "handleMessage: Failed to connect");
                                break;
                        }
                        break;


                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, BlutoothConnect.class);
                startActivity(intent);
            }
        });





        //text to speech(setting the language bangla)

        TextToSpeech tts = new TextToSpeech(this, this);
        tts.setLanguage(Locale.forLanguageTag("en-US"));
        tts.setPitch(0.8f);
        tts.setSpeechRate(1f);

        //setting up speech reconization language to bangla

        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US");
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        //Main Part of our APP

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) { }
            @Override
            public void onBeginningOfSpeech() { }
            @Override
            public void onRmsChanged(float rmsdB) { }
            @Override
            public void onBufferReceived(byte[] buffer) { }
            @Override
            public void onEndOfSpeech() { }
            @Override
            public void onError(int error) {
                /*
                try {
                    Thread.sleep(2000);
                    userChat.setText("error abr bolun "+i);
                    i++;
                    speechRecognizer.startListening(intentRecognizer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                 */

            }


            @Override
            public void onResults(Bundle results) {


                ArrayList<String> store = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                String userVoice = "";
                if (store != null) {
                    userVoice = store.get(0);

                    //TalkButton Main result Part

                    if (x == 1) {
                        userChat.setText(userVoice);
                        String finalInput = userVoice;

                        if(CONNECTING_STATUS==1) {
                            intentAction(finalInput);
                        }




                            dbQuestions.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    String Qanswer = (String) snapshot.child(finalInput).getValue();

                                    System.out.println(Qanswer);

                                    if (Qanswer != null) {
                                       try {
                                           intentAction("A");
                                       }catch (Exception e){

                                       }
                                        tts.speak(Qanswer, TextToSpeech.QUEUE_FLUSH, null);
                                        aiChat.setText(Qanswer);

                                    }
                                    else {
                                        try {
                                            Qanswer = QueryWiki(finalInput);



                                                /*
                                                try {
                                                    Thread.sleep(3000);
                                                    userChat.setText("K bolun"+i);
                                                    speechRecognizer.startListening(intentRecognizer);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }

                                                 */

                                                tts.speak(Qanswer, TextToSpeech.QUEUE_FLUSH, null);
                                                aiChat.setText(Qanswer);
                                                while (tts.isSpeaking()) {
                                                }
                                                /*
                                                try {
                                                    Thread.sleep(3000);
                                                    userChat.setText("k bolun"+i);
                                                    speechRecognizer.startListening(intentRecognizer);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }

                                                 */


                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                    }

                    //Input Questions Button Main result Part

                    else if (x == 2) {

                        userChat.setText(userVoice);
                        question = userVoice;
                    }

                    //Input Questions Button Main result part

                    else if (x == 3) {

                        aiChat.setText(userVoice);
                        dbQuestions.child(question).setValue(userVoice);
                    }
                }

            }
            @Override
            public void onPartialResults(Bundle partialResults) { }
            @Override
            public void onEvent(int eventType, Bundle params) { }
        });

    }

    // SetonClickListener functions

    public void TalkButton(View view){
        userChat.setText("k bolun");
        x=1;
        speechRecognizer.startListening(intentRecognizer);
    }
    public void QuestionButton(View view){
        userChat.setText("Q bolun");
        x=2;speechRecognizer.startListening(intentRecognizer); }
    public void AnswerButton(View view){
        userChat.setText("A bolun");
        x=3;speechRecognizer.startListening(intentRecognizer);}
    public void StopButton(View view){ speechRecognizer.stopListening();}
    @Override
    public void onInit(int status) {}

    public void shutdown(){
        aiChat.setText("okay");
    }









    /*-------------Ardiuno Code-------------*/

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null) {
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");


                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new MainActivity.ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n') {
                        readMessage = new String(buffer, 0, bytes);
                        Log.e("Arduino Message", readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    //Aurdino Command
    private void intentAction(String intent_action) {
        if (intent_action == null || TextUtils.isEmpty(intent_action)) {
            return;
        }
        switch (intent_action) {
            case "লেফট":
                connectedThread.write("A");
                break;
            case "রাইট":
                connectedThread.write("B");
                break;
            case "হাত তোল":
                connectedThread.write("C");
                break;
            case "হাত নামাও":
                connectedThread.write("D");
                break;
            case "মাথা ডানে ঘোরাও":
                connectedThread.write("E");
                break;
            case "মাথা বামে গুরাও":
                connectedThread.write("F");
                break;
            case "ডান্স করো":
                connectedThread.write("G");
                break;
            case "A":
                connectedThread.write("X");
                break;
            case "B":
                connectedThread.write("Y");
                break;
            default:break;

        }
    }
    public String QueryWiki(String wikiSearch) throws IOException {
        final String encoding = "UTF-8";
        String result = "";
        String ret="E";

        //Wait for user response
        //System.out.println("\n\nType something that you want me to search on the internet...");
        //String nextLine = scanner.nextLine();
        String searchText = wikiSearch +" simple wiki english";
        //System.out.println("Searching on the web....");

        Document google = Jsoup.connect("https://www.google.com/search?q="+searchText).get();

        Element link= google.getElementsByClass("yuRUbf").select("a").first();

        String relHref = link.attr("href"); // == "/"
        System.out.println(relHref);
        String absHref = link.attr("abs:href");
        System.out.println(absHref);



        //Get the first link about Wikipedia
        //String wikipediaURL = "https://bn.wikipedia.org/wiki/"+key;
        //System.out.println(wikipediaURL);
        String key=URLEncoder.encode(relHref.substring(relHref.lastIndexOf("/") + 1, relHref.length()), encoding);
        System.out.println(key);

        //Use Wikipedia API to get JSON File
        String wikipediaApiJSON = "https://simple.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles="+ key;

        //Let's see what it found
        //System.out.println(wikipediaURL);
        System.out.println(wikipediaApiJSON);

        //"extract":" the summary of the article
        HttpURLConnection httpcon = (HttpURLConnection) new URL(wikipediaApiJSON).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));


        //Read line by line
        String responseSB = in.lines().collect(Collectors.joining());
        in.close();

        //Print the result for us to see
        if(responseSB.contains("missing")) {
            String error = responseSB.split("missing\":\"")[1];
        }
        else {
            result = responseSB.split("extract\":\"")[1];
        }

        //Tell only the 150 first characters of the result
        String textToTell = result.length() > 1500 ? result.substring(0, 1500) : result;
        System.out.println(textToTell);


        return textToTell;
    }
}