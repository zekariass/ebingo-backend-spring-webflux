package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.RoomCreateDto;
import com.ebingo.backend.game.dto.RoomDto;
import com.ebingo.backend.game.dto.RoomUpdateDto;
import com.ebingo.backend.game.entity.Room;
import com.ebingo.backend.game.mappers.RoomMapper;
import com.ebingo.backend.game.repository.RoomRepository;
import com.ebingo.backend.game.service.state.GameStateService;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ReactiveTransactionManager transactionManager;
    private final GameStateService gameStateService;

    public RoomServiceImpl(RoomRepository roomRepository, ReactiveTransactionManager transactionManager, GameStateService gameStateService) {
        this.roomRepository = roomRepository;
        this.transactionManager = transactionManager;
        this.gameStateService = gameStateService;
    }

    @Override
    public Mono<RoomDto> createRoom(RoomCreateDto roomDto) {
        log.info("Creating room");
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        Room room = RoomMapper.toEntity(roomDto);
        Mono<RoomDto> roomMono = roomRepository.save(room)
                .doOnError(e -> log.error("Error creating room: {}", roomDto, e))
                .map(RoomMapper::toDto);

        log.info("Room created");
        return roomMono.as(operator::transactional);
    }

    @Override
    public Mono<RoomDto> getRoomById(Long id) {
//        log.info("===============================>> Getting room by id: {}", id);
        return roomRepository.findById(id)
                .onErrorMap(e -> new RuntimeException("Error getting room by id: " + id, e))
                .map(RoomMapper::toDto);
    }

    @Override
    public Flux<RoomDto> getAllRooms() {
        log.info("Getting all rooms");
        return roomRepository.findAll()
                .onErrorMap(e -> new RuntimeException("Error getting all rooms", e))
                .map(RoomMapper::toDto);
    }

    @Override
    public Mono<RoomDto> updateRoomById(Long id, RoomUpdateDto roomDto) {
        log.info("Updating room by id: {}", id);

        System.out.println("=====================================>>>: " + roomDto);
        return roomRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Room not found with id: " + id)))
                .flatMap(existingRoom -> {
                    RoomMapper.toEntity(roomDto, existingRoom); // mutate fields
                    return roomRepository.save(existingRoom);
                })
                .map(RoomMapper::toDto)
                .onErrorMap(e -> new RuntimeException("Error updating room with id: " + id, e));
    }


    @Override
    public Mono<Void> deleteRoomById(Long id) {

        Mono<Void> deleteGameState = gameStateService.deleteGameState(id)
                .doOnSuccess(deleted -> log.info("Deleted game state for roomId={} -> {}", id, deleted))
                .doOnError(e -> log.error("Error deleting game state for roomId={}", id, e))
                .then(); // convert Mono<Boolean> to Mono<Void>

        Mono<Void> deleteRoom = roomRepository.deleteById(id)
                .doOnSuccess(v -> log.info("Deleted room with id={}", id))
                .doOnError(e -> log.error("Error deleting room with id={}", id, e));

        // Run both in parallel and wait for both to complete
        return Mono.when(deleteRoom, deleteGameState);
    }

}
