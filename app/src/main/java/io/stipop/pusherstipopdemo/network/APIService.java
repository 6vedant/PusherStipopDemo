package io.stipop.pusherstipopdemo.network;


import io.stipop.pusherstipopdemo.model.model.Message;
import io.stipop.pusherstipopdemo.model.model.response.ServerResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by kehinde on 9/25/17.
 */

public interface APIService {

    @POST("messages/{room}")
    Call<ServerResponse> sendMessage(@Path("room") String room,
                                     @Body Message message);
}
