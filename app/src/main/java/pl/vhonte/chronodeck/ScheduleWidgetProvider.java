package pl.vhonte.chronodeck;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleWidgetProvider extends AppWidgetProvider {
    static final String ACTION_REFRESH = "pl.vhonte.chronodeck.REFRESH_WIDGET";
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        scheduleMinuteRefresh(context);
        for (int id : ids) updateOne(context, manager, id);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context,intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action) || Intent.ACTION_DATE_CHANGED.equals(action)) {
            updateAll(context);
        }
    }
    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ScheduleWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
        if(ids.length>0) scheduleMinuteRefresh(context);
    }
    private static void updateOne(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_schedule);
        String clock = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        views.setTextViewText(R.id.widget_clock,clock);
        EventStore store = new EventStore(context);
        List<EventStore.Event> all = store.forToday();
        EventStore.Event selected = store.currentOrNext();
        EventStore.Event next = null;
        if(selected!=null){boolean found=false;for(EventStore.Event e:all){if(found&&!EventStore.DONE.equals(e.status) && !EventStore.ENDED.equals(e.status)){next=e;break;}if(e.id==selected.id)found=true;}}
        if(selected==null){
            views.setTextViewText(R.id.widget_now_label,"TERAZ / BRAK WYDARZENIA");
            views.setTextViewText(R.id.widget_current, all.isEmpty()?"Dodaj pierwsze wydarzenie":"Wszystko zaplanowane");
            views.setTextViewText(R.id.widget_next,"Dotknij, aby otworzyć harmonogram");
            views.setProgressBar(R.id.widget_progress,100,0,false);
            setAction(context,views,R.id.widget_start,0,EventStore.RUNNING,widgetId);
            setAction(context,views,R.id.widget_end,0,EventStore.ENDED,widgetId);
            setAction(context,views,R.id.widget_done,0,EventStore.DONE,widgetId);
        } else {
            String state = EventStore.RUNNING.equals(selected.status)?"W TOKU":EventStore.DONE.equals(selected.status)?"WYKONANE":EventStore.ENDED.equals(selected.status)?"ZAKOŃCZONE": "BLOK / PLAN";
            views.setTextViewText(R.id.widget_now_label,"TERAZ / "+state+"  •  "+selected.timeRange());
            views.setTextViewText(R.id.widget_current,selected.title);
            views.setTextViewText(R.id.widget_next,next==null?"BRAK KOLEJNEGO WYDARZENIA":"NASTĘPNE  "+EventStore.clock(next.startMin)+"  "+next.title);
            int nowMin=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60+Calendar.getInstance().get(Calendar.MINUTE);
            int progress=EventStore.RUNNING.equals(selected.status)|| (nowMin>=selected.startMin && nowMin<selected.endMin) ? Math.max(0,Math.min(100,(nowMin-selected.startMin)*100/Math.max(1,selected.endMin-selected.startMin))) : 0;
            views.setProgressBar(R.id.widget_progress,100,progress,false);
            setAction(context,views,R.id.widget_start,selected.id,EventStore.RUNNING,widgetId);
            setAction(context,views,R.id.widget_end,selected.id,EventStore.ENDED,widgetId);
            setAction(context,views,R.id.widget_done,selected.id,EventStore.DONE,widgetId);
        }
        Intent open = new Intent(context,MainActivity.class);
        PendingIntent openPi=PendingIntent.getActivity(context,widgetId,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_current,openPi);
        manager.updateAppWidget(widgetId,views);
        store.close();
    }
    private static void setAction(Context c,RemoteViews views,int viewId,long eventId,String status,int widgetId){
        Intent i=new Intent(c,WidgetActionReceiver.class);i.setAction("pl.vhonte.chronodeck.ACTION_"+status);i.putExtra("event_id",eventId);i.putExtra("status",status);i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
        int request=(int)(eventId*31 + status.hashCode() + viewId*7);
        PendingIntent pi=PendingIntent.getBroadcast(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(viewId,pi);
    }
    private static void scheduleMinuteRefresh(Context context){
        Intent i=new Intent(context,ScheduleWidgetProvider.class);i.setAction(ACTION_REFRESH);
        PendingIntent pi=PendingIntent.getBroadcast(context,61024,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am!=null){
            long now=System.currentTimeMillis();
            long nextMinute=now+(60000L-(now%60000L));
            // Inexact by design: no special exact-alarm permission is requested. Android may defer this in Doze.
            am.set(AlarmManager.RTC, nextMinute, pi);
        }
    }
}
