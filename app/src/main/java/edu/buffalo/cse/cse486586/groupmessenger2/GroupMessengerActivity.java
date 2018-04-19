package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncAdapterType;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */




public class GroupMessengerActivity extends Activity {


    static final String TAG =" GroupMessengerActivity";
    static final String[] REMOTE_PORT0 = {"11108","11112","11116","11120","11124"};
    //PriorityQueue<String> queue=new PriorityQueue<String>();


    PriorityQueue<String> queue = new PriorityQueue<String>();

    ArrayList<Integer> proposedBY = new ArrayList<Integer>();

    ArrayList<Integer> proposedID = new ArrayList<Integer>();
    //  ArrayList<Socket> Csocket = new ArrayList<Socket>();

    Socket[] Csocket = new Socket[5];
    static final int SERVER_PORT = 10000;
    private  Uri mUri ;
    int counter = 0 ;
    int sequence = 0;
    ContentResolver cResolver ;
    int messageSeq =0 ;
    int avdid=0;
    String myport="";
    int count=0;
    int activeprocess = 5 ;
    int isfailed = 0;
    String failedport= "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);



        mUri =   buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        cResolver=getContentResolver();
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        myport = myPort;

        for(int i=0 ; i <5 ;i++)
        {
            System.out.println(myPort);
            if(myPort.equals(REMOTE_PORT0[i]))
                avdid=i;

        }



        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);


            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    public  Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];


            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             * void publishProgress (Progress... values)
             * This method can be invoked from doInBackground(Params...) to publish updates on the UI thread
             * while the background computation is still running. Each call to this method will trigger the
             * execution of onProgressUpdate(Progress...) on the UI thread.
             * onProgressUpdate(Progress...) will not be called if the task has been canceled.
             *
             * The accept() and close() methods provide the basic functionality of a server socket.
                     public Socket accept() throws IOException
                     public void close() throws IOException
             *
             * https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html jAVA DOCS
             * https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
             */


            String Message_Recived ;



            Socket sock =null ;
            try {

                for(;;) {

                    sock = serverSocket.accept();
                    BufferedReader in;

                    in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                    try {
                        if ((Message_Recived = in.readLine()) != null) {
                            System.out.println("Message_Recived"+Message_Recived);


                            String[] frstrcvd= Message_Recived.split(":");
                            if(frstrcvd[0].equals("failedport"))
                            {

                                activeprocess = 4;
                                //int i=0;

                                for (int i = 0; i < 5; i++) {
                                    if (REMOTE_PORT0[i].equals(frstrcvd[1])) {
                                        REMOTE_PORT0[i] = "1";
                                    }
                                }

                                Iterator itee = queue.iterator();
                                while (itee.hasNext()) {
                                    String s = (String) itee.next();
                                    String[] arr = s.split(":");
                                    System.out.println("failedport removed" + frstrcvd[1]);
                                    if (arr[5].equals(frstrcvd[1])) {
                                        queue.remove(s);

                                    }
                                }


                            }else {

                                //send mid---sequence to sender
                                sequence = sequence + 1;

                                System.out.println("server port" + myport);
                                StringBuilder sb = new StringBuilder();
                                String delim = ":";
                                sb.append(sequence);
                                sb.append(delim);

                                sb.append("000");
                                sb.append(delim);
                                sb.append(REMOTE_PORT0[avdid]);
                                sb.append(delim);
                                sb.append(frstrcvd[0]);
                                sb.append(delim);
                                sb.append(frstrcvd[1].trim());
                                sb.append(delim);
                                sb.append(frstrcvd[2]);


                                queue.add(sb.toString());
                                System.out.println("Added to queue" + sb.toString());

                                //sending proposed sequence

                                StringBuilder sb1 = new StringBuilder();
                                sb1.append(sequence);
                                sb1.append(delim);
                                sb1.append(REMOTE_PORT0[avdid]);

                                System.out.println("sb1.toString()" + sb1.toString());
                                PrintWriter out =
                                        new PrintWriter(sock.getOutputStream(), true);
                                out.println(sb1.toString());
                                System.out.println("sending prop seq" + sb1.toString());
                                out.flush();


                                String maxseq = in.readLine();
                                System.out.println(sock);
                                if (maxseq != null) {
                                    {
                                        System.out.println("max seq" + maxseq);
                                        String[] g = maxseq.split(":");

                                        sequence = Integer.parseInt(g[0]);

                                        int msgid = Integer.parseInt(g[2]);
                                        int agreedseq = Integer.parseInt(g[0]);
                                        int sentby = Integer.parseInt(g[1]);

                                        Iterator itee = queue.iterator();

                                        if (g[(g.length) - 1].equals("failedreport")) {
                                            String failedreport = g[g.length - 2];
                                            activeprocess = 4;
                                            //int i=0;

                                            for (int i = 0; i < 5; i++)
                                                if (REMOTE_PORT0[i].equals(failedreport)) {
                                                    REMOTE_PORT0[i] = "1";
                                                }

                                     while (itee.hasNext()) {
                                                String s = (String) itee.next();
                                                String[] arr = s.split(":");
                                                System.out.println("failedport removed" + failedreport);
                                                if (arr[5].equals(failedreport)) {
                                                    queue.remove(s);

                                                }
                                            }


                                        }




                                        sequence = sequence >= agreedseq ? sequence : agreedseq; //change propsed id
                                        String[] sarr;
                                        Iterator ite = queue.iterator();
                                        while (ite.hasNext()) {
                                            String s = (String) ite.next();
                                            sarr = s.split(":");
                                            System.out.println("matching" + msgid + " " + Integer.parseInt(sarr[3]) + " " + frstrcvd[2] + " " + sarr[5]);
                                            if (msgid == Integer.parseInt(sarr[3]) && (frstrcvd[2].equals(sarr[5]))) {
                                                System.out.println("matched");
                                                if (agreedseq >= Integer.parseInt(sarr[0])) {
                                                    sarr[0] = g[0];
                                                    sarr[2] = g[1];

                                                } else if (agreedseq == Integer.parseInt(sarr[0])) {
                                                    if (sentby < Integer.parseInt(sarr[2])) {
                                                        sarr[2] = g[1];

                                                    }

                                                }
                                                System.out.println("4007" + sarr[1]);
                                                sarr[1] = "999";
                                                System.out.println(sarr[1]);
                                                System.out.println("queue:" + queue);
                                                queue.remove((Object) s);
                                                StringBuilder ss = new StringBuilder();
                                                String news = "";
                                                for (int j = 0; j < sarr.length; j++) {
                                                    System.out.println("j v " + sarr[j]);
                                                    ss.append(sarr[j]);
                                                    if (j != sarr.length - 1)
                                                        ss.append(":");

                                                }
                                                System.out.println("412" + queue.size());
                                                queue.add(ss.toString());
                                                System.out.println(ss.toString());
                                                System.out.println("412" + queue.size());
                                                System.out.println("queue:" + queue);
                                                break;
                                            }
                                        }


                                        String f = "";


                                        while (!queue.isEmpty()) {
                                            System.out.println("372" + queue.size());
                                            System.out.println(queue.peek());
                                            String msg = (String) queue.peek();
                                            String[] splitmsg = msg.split(":");
                                            System.out.println("426 " + splitmsg[1]);
                                            if (splitmsg[1].equals("999")) {

                                                System.out.println("key  " + count);
                                                System.out.println("value   " + splitmsg[4]);
                                                ContentValues values = new ContentValues();
                                                values.put("key", count);
                                                count++;
                                                values.put("value", splitmsg[4]);
                                                f = splitmsg[0] + ":" + splitmsg[2] + ":" + splitmsg[4];

                                                cResolver.insert(mUri, values);
                                                queue.poll();
                                            } else
                                                break;
                                        }


                                        publishProgress(f);


                                    }

                                } else {

                                    activeprocess = 4;


                                    for (int i = 0; i < 5; i++)
                                        if (REMOTE_PORT0[i].equals(frstrcvd[2])) {
                                            REMOTE_PORT0[i] = "1";
                                        }

                                    Iterator itee = queue.iterator();
                                    while (itee.hasNext()) {
                                        String s = (String) itee.next();
                                        String[] arr = s.split(":");

                                        if (arr[5].equals(frstrcvd[2])) {
                                            queue.remove(s);

                                        }
                                    }

                                    String f = "";


                                    while (!queue.isEmpty()) {

                                        System.out.println(queue.peek());
                                        String msg = (String) queue.peek();
                                        String[] splitmsg = msg.split(":");
                                        System.out.println("426 " + splitmsg[1]);
                                        if (splitmsg[1].equals("999")) {


                                            ContentValues values = new ContentValues();
                                            values.put("key", count);
                                            count++;
                                            values.put("value", splitmsg[4]);
                                            f = splitmsg[0] + ":" + splitmsg[2] + ":" + splitmsg[4];

                                            cResolver.insert(mUri, values);
                                            queue.poll();
                                        } else
                                            break;
                                    }


                                    publishProgress(f);




                                    for (int i = 0; i < REMOTE_PORT0.length; i++)

                                    {
                                        System.out.println("REMOTE_PORT0[i].equals(\"1\" ) " + REMOTE_PORT0[i].equals("1"));

                                        if (!REMOTE_PORT0[i].equals("1")) {
                                            try {

                                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                        Integer.parseInt(REMOTE_PORT0[i]));

                                                StringBuilder sb5 = new StringBuilder();
                                                String delimm = ":";
                                                sb5.append("failedport");
                                                sb5.append(delimm);
                                                sb5.append(frstrcvd[2]);

                                                System.out.println(sb5.toString());
                                                PrintWriter iout =
                                                        new PrintWriter(socket.getOutputStream(), true);
                                                iout.println(sb5.toString());
                                                iout.flush();

                                            } catch (Exception e) {


                                            }
                                        }


                                    }




                                }




                            }
                        } else {
                            // sock.close();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "No message ");
                    }


                }







            } catch (IOException e) {
                Log.e(TAG, "not able to accept request  ");
            }






            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0];
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived+"\n");

            System.out.println("ON progress update");
            System.out.println(strReceived);
            try {



                messageSeq++;
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;

        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            counter++;


            for (int i = 0; i < REMOTE_PORT0.length; i++)

            {
                System.out.println("REMOTE_PORT0[i].equals(\"1\" ) "+REMOTE_PORT0[i].equals("1"));

                if(!REMOTE_PORT0[i].equals("1")) {
                    try {

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT0[i]));

                        Csocket[i]=socket;
                        socket.setSoTimeout(5000);

                        StringBuilder sb = new StringBuilder();
                        String delim = ":";
                        sb.append(counter);
                        sb.append(delim);
                        sb.append(msgs[0].trim());
                        sb.append(delim);
                        sb.append(myport);
                        System.out.println("Message Sent  " + sb.toString());

                        PrintWriter out =
                                new PrintWriter(socket.getOutputStream(), true);
                        out.println(sb.toString());
                        out.flush();

                        BufferedReader client = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        String msg = client.readLine();
                        if (msg != null) {

                            String[] m = msg.split(":");
                            proposedID.add(Integer.parseInt(m[0]));
                            proposedBY.add(Integer.parseInt(m[1]));

                        }
                        else{


                        }
                    } catch (SocketTimeoutException e) {

                        activeprocess=4;
                        System.out.println(e);
                        failedport = REMOTE_PORT0[i];
                        System.out.println(failedport);
                        REMOTE_PORT0[i] = "1";
                        isfailed = 1;


                    } catch (IOException e){

                        System.out.println("IO EXCEPTION");
                        activeprocess=4;
                        // System.out.println(e);
                        failedport = REMOTE_PORT0[i];
                        REMOTE_PORT0[i] = "1";
                        isfailed = 1;
                        // continue;
                    }
                }
                else
                    continue;



            }
            int max = 0;



            int leastport =0 ;

            if(proposedID.size()==activeprocess) {
                System.out.println("all rcvd");

                for (int j = 0; j < proposedID.size(); j++) {

                    if (max < proposedID.get(j)) {
                        max = proposedID.get(j);
                        leastport = proposedBY.get(j);
                    }
                    if(max==proposedID.get(j))
                    {
                        if(proposedBY.get(j)<leastport)
                            leastport=proposedBY.get(j);
                    }
                }




                StringBuilder sb2 = new StringBuilder();
                String delim = ":";
                sb2.append(max);
                sb2.append(delim);
                sb2.append(leastport);
                sb2.append(delim);
                sb2.append(counter);
                if(isfailed==1){
                    sb2.append(delim);
                    sb2.append(failedport);
                    sb2.append(delim);
                    sb2.append("failedreport");
                    isfailed=0;
                }




                for (int j = 0; j < 5; j++) {
                    if (!REMOTE_PORT0[j].equals("1")) {
                        try {

                            PrintWriter out =
                                    new PrintWriter(Csocket[j].getOutputStream(), true);
                            System.out.println("sb2.toString()"+sb2.toString());
                            out.println(sb2.toString());
                            out.flush();
                        }catch (Exception e6) {

                        }

                    }
                }
            }
            proposedID.clear();
            proposedBY.clear();


            return null;
        }
    }

}









