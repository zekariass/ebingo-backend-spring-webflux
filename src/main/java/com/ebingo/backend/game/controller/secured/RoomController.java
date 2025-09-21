package com.ebingo.backend.game.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.game.dto.RoomCreateDto;
import com.ebingo.backend.game.dto.RoomDto;
import com.ebingo.backend.game.dto.RoomUpdateDto;
import com.ebingo.backend.game.service.CardPoolService;
import com.ebingo.backend.game.service.RedisPublisher;
import com.ebingo.backend.game.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/secured/rooms")
@Tag(name = "Room Secured Controller", description = "Room Secured Controller")
public class RoomController {
    private final RoomService roomService;
    private final CardPoolService cardService;
    private final RedisPublisher publisher;

    public RoomController(RoomService roomService, CardPoolService cardService, RedisPublisher publisher) {
        this.roomService = roomService;
        this.cardService = cardService;
        this.publisher = publisher;
    }

    @PostMapping
    @Operation(summary = "Create room", description = "Create room")
    public Mono<ResponseEntity<ApiResponse<RoomDto>>> createRoom(
            @Parameter(required = true, description = "Room") @Valid @RequestBody RoomCreateDto roomDto,
            ServerWebExchange exchange
    ) {

        return roomService.createRoom(roomDto)
                .map(createdRoom -> ApiResponse.<RoomDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Room created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(createdRoom)
                        .build()
                )
                .map(response -> ResponseEntity.status(201).body(response));
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Get room by ID")
    public Mono<ResponseEntity<ApiResponse<RoomDto>>> getRoomById(
            @Parameter(required = true, description = "Room ID") @RequestParam Long id,
            ServerWebExchange exchange) {
        return roomService.getRoomById(id)
                .map(room -> ApiResponse.<RoomDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(room)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @GetMapping
    @Operation(summary = "Get all rooms", description = "Get all rooms")
    public Mono<ResponseEntity<ApiResponse<List<RoomDto>>>> getAllRooms(ServerWebExchange exchange) {
        return roomService.getAllRooms()
                .collectList()
                .map(rooms -> ApiResponse.<List<RoomDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Rooms retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(rooms)
                        .build())
                .map(ResponseEntity::ok);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update room by ID", description = "Update room by ID")
    public Mono<ResponseEntity<ApiResponse<RoomDto>>> updateRoomById(
            @Parameter(required = true, description = "Room ID") @PathVariable Long id,
            @Parameter(required = true, description = "Room") @Valid @RequestBody RoomUpdateDto roomDto,
            ServerWebExchange exchange) {
        return roomService.updateRoomById(id, roomDto)
                .map(updatedRoom -> ApiResponse.<RoomDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room updated successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(updatedRoom)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room by ID", description = "Delete room by ID")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteRoomById(
            @Parameter(required = true, description = "Room ID") @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        return roomService.deleteRoomById(id)
                .then(Mono.fromSupplier(() -> ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room deleted successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .build()
                ))
                .map(ResponseEntity::ok);
    }

//    @GetMapping("/{roomId}/pool")
//    public Mono<Set<CardInfo>> getPool(@PathVariable Long roomId, @RequestParam Long gameId, @RequestParam int capacity) {
//        return cardService.ensurePool(roomId, capacity);
//    }
//
//    @PostMapping("/{roomId}/regenerate")
//    public Mono<Set<CardInfo>> regenPool(@PathVariable Long roomId, @RequestParam Long gameId, @RequestParam int capacity) {
//        return cardService.regenerate(roomId, capacity)
//                .flatMap(cards -> publisher.publishEvent(RedisKeys.roomChannel(roomId), Map.of("type", "cardPoolGenerated", "cards", cards)).thenReturn(cards));
//    }
}
