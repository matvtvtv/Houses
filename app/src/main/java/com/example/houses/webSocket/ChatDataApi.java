package com.example.houses.webSocket;

import com.example.houses.model.ChatData;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ChatDataApi {

    @GET("api/chats_data/get_chats_users/{chatLogin}")
    Call<List<ChatData>> getChatUsers(@Path("chatLogin") String chatLogin);
}