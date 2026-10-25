package pl.vhonte.chronodeck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long id=intent.getLongExtra("event_id",0);
        String status=intent.getStringExtra("status");
        if(id>0 && status!=null){EventStore store=new EventStore(context);store.setStatus(id,status);store.close();}
        ScheduleWidgetProvider.updateAll(context);
    }
}
