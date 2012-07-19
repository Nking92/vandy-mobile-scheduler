package com.vmat;

import com.actionbarsherlock.app.SherlockActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import org.json.JSONObject;
import org.json.JSONException;

public class MyVmMain extends SherlockActivity
{
	Button submit;
	EditText username;
	EditText password;
	TextView responseText;
	UserInfoDownloader backTask = null;

    @Override
	public void onCreate(Bundle savedInstantState){
		setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        super.onCreate(savedInstantState);
		setContentView(R.layout.my_vm_activity_main);

		// If the user is already logged in, his/her info will be stored, so
		// open the next activity with the info loaded.
		SharedPreferences prefs = getSharedPreferences("user-settings", MODE_PRIVATE);
		String userInfo = prefs.getString("userInfo", "empty");
		if ( !userInfo.equals("empty") ){
			
			startActivity(new Intent(MyVmMain.this, MyVmUserPage.class));
			finish();
		}

		// The user is not logged in, so continue making the login screen.
		submit = (Button)findViewById(R.id.submit);
		username = (EditText)findViewById(R.id.username);
		password = (EditText)findViewById(R.id.password);
		responseText = (TextView)findViewById(R.id.responseText);

		submit.setOnClickListener(new View.OnClickListener(){
			public void onClick(View view){
				String user = username.getText().toString();
				String pass = password.getText().toString();
				if (user.length() == 0 || pass.length() == 0){
					// Make toast prompting for required fields
				}
				backTask = new UserInfoDownloader();
				backTask.execute(user, pass);
			}
		});
	
	}


	private class UserInfoDownloader extends AsyncTask<String, Void, Void>{

		ProgressDialog dialog = null;
		@Override
		protected void onPreExecute(){
			// make progress dialog
			dialog = new ProgressDialog(MyVmMain.this);
			dialog.show(MyVmMain.this, "", "Loading your info...", true, false);
		}

		@Override
		protected Void doInBackground(String... credentials){
			URL url;
			HttpURLConnection conn;

			try{
				url = new URL("http://70.138.50.84/sessions.json");

				String params = "username=" + URLEncoder.encode(credentials[0], "UTF-8") +
					"&password=" + URLEncoder.encode(credentials[1], "UTF-8");
				Log.i("MyVmActivityMain", params);

				// Send username and password to server
				conn = (HttpURLConnection)url.openConnection();
				conn.setDoOutput(true);
				conn.setFixedLengthStreamingMode(params.getBytes().length);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(params);
				out.close();

				// Receive JSON response
				String response = "";
				Scanner inStream = new Scanner(conn.getInputStream());
				while (inStream.hasNextLine())
					response += inStream.nextLine();

				////////////////////
				// We have received a response, so take that response and store it.
				JSONObject jsonObject = new JSONObject(response);
						
				responseText.setText((String)jsonObject.get("email"));

				SharedPreferences prefs = getPreferences(MODE_PRIVATE);	
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("userInfo", response);
				editor.commit();

			// The response indicates a bad username and pass.
			}catch(JSONException e){
				// make a toast to the user to reenter their username & pass
			}catch(MalformedURLException e){
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing){
			// close progress dialog
			dialog.dismiss();
		}
	}

}
