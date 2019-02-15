package lmu.pms.stila.util;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Toast;

import lmu.pms.stila.R;

public class BodySensorPermissionActivity extends WearableActivity {

    private TextView mTextView;
    private static final int SENSOR_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_sensor_permission);

        //mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        //setAmbientEnabled();
        requestSensorPermission();
    }


    private void requestSensorPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BODY_SENSORS) ==
                PackageManager.PERMISSION_GRANTED)
            return;

        if (ActivityCompat.shouldShowRequestPermissionRationale
                (this, Manifest.permission.BODY_SENSORS)) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }
        //And finally ask for the permission
        ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.BODY_SENSORS}, SENSOR_PERMISSION_CODE);
    }

    //This method will be called when the user will tapon allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Checking the request code of our request
        if (requestCode == SENSOR_PERMISSION_CODE) {

            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                //Displaying a toast
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                finish();
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(this, "To use the Stila Watch Face you have to grant the permission", Toast.LENGTH_LONG).show();
                requestSensorPermission();
            }
        }
    }
}

