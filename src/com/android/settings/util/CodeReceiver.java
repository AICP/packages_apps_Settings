package com.android.settings.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;

public class CodeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (new File("/data/local/bootanimation.user").exists()) {
            context.startService(new Intent(context,
                    ExternalCommandService.class)
                    .putExtra("cmd",
                            "mv /data/local/bootanimation.user /data/local/bootanimation.zip"));
        } else {
            context.startService(new Intent(context,
                    ExternalCommandService.class)
                    .putExtra("cmd",
                            "rm /data/local/bootanimation.zip"));
        }
        Toast.makeText(context, ":(", Toast.LENGTH_SHORT).show();
        context.startService(new Intent(context,
                ExternalCommandService.class)
                .putExtra("cmd",
                        "chmod 644 /data/local/bootanimation.zip"));
    }
}
