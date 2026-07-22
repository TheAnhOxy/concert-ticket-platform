package com.ticket.booking.dto.request;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class RaceConditionTest {

    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();

        // Data của Người A (Idempotency Key A, User 2)
        String bodyUserA = """
                {
                  "userId": 2, "concertId": 1, "ticketCategoryId": 1, "quantity": 1,
                  "unitPrice": 1000000.00, "totalAmount": 1000000.00, "finalAmount": 1000000.00,
                  "idempotencyKey": "RACE-KEY-A2", "contactName": "Người A",
                  "contactEmail": "a@gmail.com", "contactPhone": "0123456789", "paymentMethod": "MOCK"
                }
                """;

        // Data của Người B (Idempotency Key B, User 3)
        String bodyUserB = """
                {
                  "userId": 3, "concertId": 1, "ticketCategoryId": 1, "quantity": 1,
                  "unitPrice": 1000000.00, "totalAmount": 1000000.00, "finalAmount": 1000000.00,
                  "idempotencyKey": "RACE-KEY-B2", "contactName": "Người B",
                  "contactEmail": "b@gmail.com", "contactPhone": "0987654321", "paymentMethod": "MOCK"
                }
                """;

        HttpRequest requestA = buildRequest(bodyUserA);
        HttpRequest requestB = buildRequest(bodyUserB);

        System.out.println("Bắt đầu bắn 2 request CÙNG LÚC...");

        // Dùng CompletableFuture để 2 luồng chạy song song thực sự
        CompletableFuture<Void> taskA = CompletableFuture.runAsync(() -> sendRequest(client, requestA, "Người A"));
        CompletableFuture<Void> taskB = CompletableFuture.runAsync(() -> sendRequest(client, requestB, "Người B"));

        // Chờ cả 2 hoàn thành
        CompletableFuture.allOf(taskA, taskB).join();
        System.out.println("Test hoàn tất. Hãy check log server và database!");
    }

    private static HttpRequest buildRequest(String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8083/bookings/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    private static void sendRequest(HttpClient client, HttpRequest request, String name) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(name + " nhận kết quả: " + response.statusCode() + " - " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}