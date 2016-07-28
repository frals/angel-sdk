package se.frals.sense.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import se.frals.sense.persistence.SenseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class ShareHelper {

    public static void shareDatabase(Context context) throws IOException {
        SenseDatabase senseDatabase = new SenseDatabase(context.getApplicationContext());
        File privDb = new File(senseDatabase.getReadableDatabase().getPath());
        File requestFile = new File(context.getFilesDir(), "databases/" + SenseDatabase
                .DATABASE_NAME);

        requestFile.getParentFile().mkdirs();
        if (!requestFile.exists()) {
            requestFile.createNewFile();
        }

        requestFile.setWritable(true);

        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(privDb).getChannel();
            outputChannel = new FileOutputStream(requestFile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }

        Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(context, "se.frals.sense.fileprovider",
                    requestFile);
        } catch (IllegalArgumentException e) {
            Log.e("File Selector", "The selected file can't be shared: " + requestFile.getAbsolutePath());
        }
        if (fileUri != null) {
            Intent shareIntent = new Intent("se.frals.sense.ACTION_RETURN_FILE");
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setDataAndType(fileUri, context.getContentResolver().getType(fileUri));
            context.startActivity(Intent.createChooser(shareIntent, "Share to"));
        }
    }
}
