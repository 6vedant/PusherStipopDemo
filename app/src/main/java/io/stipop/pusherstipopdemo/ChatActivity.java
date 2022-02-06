package io.stipop.pusherstipopdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.stipop.Stipop;
import io.stipop.StipopDelegate;
import io.stipop.extend.StipopImageView;
import io.stipop.model.SPPackage;
import io.stipop.model.SPSticker;
import io.stipop.pusherstipopdemo.adapter.ChatRecyclerAdapter;
import io.stipop.pusherstipopdemo.model.model.Message;
import io.stipop.pusherstipopdemo.model.model.response.ServerResponse;
import io.stipop.pusherstipopdemo.network.APIService;
import io.stipop.pusherstipopdemo.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity implements StipopDelegate {

    @BindView(R.id.edt_chat_message)
    EditText edt_chat_message;
    @BindView(R.id.fab_send_message)
    FloatingActionButton fab_send_message;
    @BindView(R.id.chat_recycler_view)
    RecyclerView chat_recycler_view;
    @BindView(R.id.progress)
    ProgressBar progress;

    private String chat_room_name;
    private String username;
    private ChatRecyclerAdapter chatRecyclerAdapter;
    private ArrayList<Message> messageList=new ArrayList<>();
    private String LIST = "list";
    private Channel channel;


    StipopImageView stipopIM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        if (getIntent()!=null) {
            chat_room_name = getIntent().getStringExtra(Constants.CHAT_ROOM_NAME_EXTRA);
            username = getIntent().getStringExtra(Constants.USER_NAME_EXTRA);
        }
        if (getSupportActionBar()!=null && chat_room_name!=null) {
            getSupportActionBar().setTitle(chat_room_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        if (savedInstanceState!=null) messageList=savedInstanceState.getParcelableArrayList(LIST);

        chatRecyclerAdapter=new ChatRecyclerAdapter(this,messageList,username);
        chat_recycler_view.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        chat_recycler_view.setItemAnimator(new DefaultItemAnimator());
        chat_recycler_view.setAdapter(chatRecyclerAdapter);

        edt_chat_message.addTextChangedListener(textWatcher);

        //Pusher Connection
        PusherOptions options = new PusherOptions();
        options.setCluster("eu");
        Pusher pusher = new Pusher("5a78ac20c7997388e791", options);

        channel = pusher.subscribe(chat_room_name);
        channel.bind("new_message", subscriptionEventListener);

        pusher.connect();

        // stipop methods
        stipopIM = (StipopImageView) findViewById(R.id.stipopIV);
        Stipop.Companion.connect(this, stipopIM, "1234", "en", "US", this);

        stipopIM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Stipop.Companion.showKeyboard();
            }
        });
    }

    SubscriptionEventListener subscriptionEventListener=new SubscriptionEventListener() {
        @Override
        public void onEvent(String channelName, String eventName, final String data) {
            Gson gson=new Gson();
            final Message message = gson.fromJson(data,Message.class);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showMessageAdded(message);
                }
            });
        }
    };

    @OnClick(R.id.fab_send_message)
    void fabSendMessageClicked(){
        String message=edt_chat_message.getText().toString();
        if (!TextUtils.isEmpty(message)) {

            sendMessage(message);
        }
    }

    private void sendMessage(final String message) {
        showProgress();

        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .baseUrl(Constants.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create());

        APIService apiService= builder.build().create(APIService.class);
        Call<ServerResponse> call=apiService.sendMessage(chat_room_name,
                new Message(message,username));
        call.enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                if (response.body()!=null){
                    if (response.body().getSuccess()!=null) {
                        hideProgress();
                        edt_chat_message.setText("");
                    }
                }
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                hideProgress();
            }
        });

    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
        fab_send_message.setVisibility(View.INVISIBLE);
    }

    private void hideProgress() {
        progress.setVisibility(View.GONE);
        fab_send_message.setVisibility(View.VISIBLE);
    }

    public void showMessageAdded(Message message){
        chatRecyclerAdapter.addMessage(message);
        chat_recycler_view.scrollToPosition((chatRecyclerAdapter.getItemCount()-1));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(LIST,chatRecyclerAdapter.getMessageList());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private TextWatcher textWatcher=new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.toString().isEmpty()) {
                fab_send_message.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.grey)));
            }
            else {
                fab_send_message.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (channel.isSubscribed())channel.unbind("new_message",subscriptionEventListener);
    }

    @Override
    public boolean canDownload(@NotNull SPPackage spPackage) {
        return true;
    }

    @Override
    public boolean onStickerSelected(@NotNull SPSticker spSticker) {
        sendMessage(spSticker.getStickerImg());
        return true;
    }
}
