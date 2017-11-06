package com.vnspectre.waterreminder.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

/**
 * Created by Spectre on 11/5/17.
 */

public class WaterReminderFirebaseJobService extends JobService {
    private AsyncTask mBackground;

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters job) {
        mBackground = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                Context context = WaterReminderFirebaseJobService.this;
                ReminderTasks.executeTask(context, ReminderTasks.ACTION_CHARGING_REMINDER);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                jobFinished(job, false);
            }
        };
        mBackground.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        if (mBackground != null ) {
            mBackground.cancel(true);
        }
        return true;
    }
}
