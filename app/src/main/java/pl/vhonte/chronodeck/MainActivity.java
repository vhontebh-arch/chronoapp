package pl.vhonte.chronodeck;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private EventStore store;
    private LinearLayout list;
    private TextView clockView;
    private int startMin = 9*60, endMin = 10*60;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (store != null) refresh();
            clockHandler.postDelayed(this, 60_000L);
        }
    };
    private final int bg = Color.rgb(12, 19, 27), panel = Color.rgb(20, 31, 42), mint = Color.rgb(86, 224, 194), text = Color.rgb(242, 247, 250), muted = Color.rgb(151, 171, 185);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store = new EventStore(this); buildUi(); refresh();
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(20),dp(18),dp(12)); root.setBackgroundColor(bg);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("CHRONODECK  /  LOCAL SYSTEM", 12, mint, true); header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        clockView = label("--:--", 25, text, true); header.addView(clockView); root.addView(header);
        TextView subtitle = label(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Calendar.getInstance().getTime()).toUpperCase(Locale.getDefault()), 11, muted, true); subtitle.setPadding(0,dp(5),0,dp(15)); root.addView(subtitle);
        Button add = new Button(this); add.setText("+  DODAJ WYDARZENIE"); add.setTextColor(bg); add.setBackground(tintBg(mint, 10)); add.setOnClickListener(v -> showAddDialog()); root.addView(add, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView heading = label("OŚ CZASU  /  DZISIAJ", 12, mint, true); heading.setPadding(0,dp(20),0,dp(10)); root.addView(heading);
        ScrollView scroll = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        TextView footer = label("OFFLINE • DANE LOKALNE • BRAK KONTA", 10, muted, true); footer.setGravity(Gravity.CENTER); footer.setPadding(0,dp(12),0,0); root.addView(footer);
        setContentView(root);
    }
    private void refresh() {
        clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime()));
        list.removeAllViews(); List<EventStore.Event> events = store.forToday();
        if (events.isEmpty()) {
            TextView empty = label("BRAK ZAPLANOWANYCH WYDARZEŃ\n\nDodaj pierwszy blok czasu przyciskiem powyżej.", 14, muted, false); empty.setPadding(dp(8),dp(24),dp(8),dp(24)); list.addView(empty);
        }
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60 + Calendar.getInstance().get(Calendar.MINUTE); boolean lineAdded = false;
        for (EventStore.Event e : events) {
            if (!lineAdded && now < e.startMin) { list.addView(nowLine()); lineAdded=true; }
            list.addView(eventCard(e, now));
            if (!lineAdded && now >= e.startMin && now <= e.endMin) { list.addView(nowLine()); lineAdded=true; }
        }
        if (!lineAdded) list.addView(nowLine());
        ScheduleWidgetProvider.updateAll(this);
    }
    private View nowLine() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,dp(5),0,dp(5));
        View dot = new View(this); dot.setBackground(tintBg(mint, 50)); row.addView(dot, new LinearLayout.LayoutParams(dp(8),dp(8)));
        TextView t = label("  TERAZ  " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime()), 11, mint, true); row.addView(t);
        View line = new View(this); line.setBackgroundColor(mint); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(1),1); lp.leftMargin=dp(8); row.addView(line,lp); return row;
    }
    private View eventCard(EventStore.Event e, int now) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(12),dp(10),dp(12),dp(10)); card.setBackground(tintBg(panel, 10));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(8); card.setLayoutParams(cp);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView times = label(e.timeRange(), 12, mint, true); top.addView(times);
        TextView status = label("  /  "+statusName(e.status), 10, EventStore.DONE.equals(e.status)?muted:mint, true); top.addView(status); card.addView(top);
        TextView title = label(e.title, 16, EventStore.DONE.equals(e.status)?muted:text, true); title.setPadding(0,dp(5),0,dp(8)); card.addView(title);
        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.END);
        Button start = miniButton("START"); start.setOnClickListener(v -> act(e.id, EventStore.RUNNING)); buttons.addView(start);
        Button end = miniButton("STOP"); end.setOnClickListener(v -> act(e.id, EventStore.ENDED)); buttons.addView(end);
        Button done = miniButton("DONE ✓"); done.setOnClickListener(v -> act(e.id, EventStore.DONE)); buttons.addView(done);
        card.addView(buttons);
        card.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setTitle(e.title).setItems(new String[]{"Usuń wydarzenie"}, (d,w) -> { store.getWritableDatabase().delete("events","_id=?",new String[]{String.valueOf(e.id)}); refresh(); }).setNegativeButton("Anuluj",null).show(); return true; });
        return card;
    }
    private void act(long id, String status) { store.setStatus(id,status); refresh(); Toast.makeText(this, statusName(status), Toast.LENGTH_SHORT).show(); }
    private void showAddDialog() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),dp(8),dp(20),0);
        EditText titleInput = new EditText(this); titleInput.setSingleLine(true); titleInput.setHint("Nazwa wydarzenia"); titleInput.setTextColor(text); titleInput.setHintTextColor(muted); form.addView(titleInput);
        LinearLayout times = new LinearLayout(this); times.setGravity(Gravity.CENTER_VERTICAL);
        Button start = new Button(this); Button end = new Button(this); updateTimeButton(start,"Start",startMin); updateTimeButton(end,"Koniec",endMin);
        start.setOnClickListener(v -> new TimePickerDialog(this,(view,h,m)->{startMin=h*60+m;updateTimeButton(start,"Start",startMin);},startMin/60,startMin%60,true).show());
        end.setOnClickListener(v -> new TimePickerDialog(this,(view,h,m)->{endMin=h*60+m;updateTimeButton(end,"Koniec",endMin);},endMin/60,endMin%60,true).show());
        times.addView(start,new LinearLayout.LayoutParams(0,dp(50),1)); times.addView(end,new LinearLayout.LayoutParams(0,dp(50),1)); form.addView(times);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("NOWY BLOK CZASU").setView(form)
                .setPositiveButton("ZAPISZ", null).setNegativeButton("ANULUJ", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title=titleInput.getText().toString().trim();
            if(title.isEmpty()){titleInput.setError("Wpisz nazwę wydarzenia");return;}
            if(endMin<=startMin){Toast.makeText(this,"Koniec musi być późniejszy niż początek",Toast.LENGTH_LONG).show();return;}
            store.add(title,startMin,endMin); refresh(); dialog.dismiss();
        }));
        dialog.show();
    }
    private void updateTimeButton(Button b,String label,int mins){b.setText(label+"  "+EventStore.clock(mins));}
    private Button miniButton(String s){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(5),0,dp(5),0);b.setTextColor(mint);return b;}
    private TextView label(String s,int size,int color,boolean mono){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(mono)t.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);return t;}
    private android.graphics.drawable.Drawable tintBg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private String statusName(String s){if(EventStore.RUNNING.equals(s))return "W TOKU";if(EventStore.DONE.equals(s))return "WYKONANE";if(EventStore.ENDED.equals(s))return "ZAKOŃCZONE";return "PLAN";}
    @Override protected void onResume(){super.onResume();if(store!=null){refresh();clockHandler.removeCallbacks(clockTick);clockHandler.postDelayed(clockTick,60_000L);}}
    @Override protected void onPause(){clockHandler.removeCallbacks(clockTick);super.onPause();}
    @Override protected void onDestroy(){clockHandler.removeCallbacks(clockTick);if(store!=null){store.close();store=null;}super.onDestroy();}
}
