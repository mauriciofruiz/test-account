package com.example.testaccount.service;

import com.example.testaccount.api.TestApi;
import com.example.testaccount.constants.Constants;
import com.example.testaccount.dto.AccountStatusDto;
import com.example.testaccount.dto.MovementDto;
import com.example.testaccount.exceptions.CustomException;
import com.example.testaccount.model.Account;
import com.example.testaccount.model.Movement;
import com.example.testaccount.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MovementService {

    private final MovementRepository movementRepository;
    private final AccountService accountService;
    private final AccountStatusService accountStatusService;

    /**
     * INICIA: Métodos básicos para el CRUD
     */

    public Mono<Void> save(Movement movement) {
        return movementRepository.save(movement).then();
    }

    public Mono<Void> update(Integer id, Movement movement) {
        return movementRepository.findById(id)
                .flatMap(movement1 -> {
                    movement1.setMovementDate(movement.getMovementDate());
                    movement1.setAccountMovementId(movement.getAccountMovementId());
                    movement1.setMovementValue(movement.getMovementValue());
                    movement1.setMovementBalance(movement.getMovementBalance());
                    return movementRepository.save(movement1);
                }).then();
    }

    public Mono<Movement> getMovementById(Integer id) {
        return movementRepository.findById(id);
    }

    public Flux<Movement> getMovements() {
        return movementRepository.findAll();
    }

    public Mono<Void> deleteMovementById(Integer id) {
        return movementRepository.deleteById(id);
    }

    /**
     * FINALIZA: Métodos básicos para el CRUD
     */

    public Mono<Void> createMovement(MovementDto movementDto) {
        Mono<BigDecimal> previousBalance = movementRepository.findFirstByAccountMovementIdOrderByMovementDateDesc(movementDto.getAccountMovementId())
                .map(Movement::getMovementBalance)
                .switchIfEmpty(accountService.getAccountById(movementDto.getAccountMovementId()).map(Account::getInitialBalance));

        return previousBalance.flatMap(balance -> {
            BigDecimal currentBalance = balance.add(movementDto.getMovementValue());

            if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
                return Mono.error(new CustomException(HttpStatus.BAD_REQUEST, Constants.NO_BALANCE_MESSAGE));
            }
            Movement movement = Movement.builder()
                    .movementDate(LocalDateTime.now())
                    .accountMovementId(movementDto.getAccountMovementId())
                    .movementValue(movementDto.getMovementValue())
                    .movementBalance(currentBalance)
                    .build();
            return movementRepository.save(movement).then();
        });
    }

    public Flux<AccountStatusDto> getAccountStatus(LocalDateTime startDate, LocalDateTime endDate, Integer clientId) {
        if(startDate == null || endDate == null || clientId == null) {
            return Flux.error(new CustomException(HttpStatus.BAD_REQUEST, Constants.MISSING_PARAMETER_MESSAGE));
        }
        return movementRepository.getAccountStatus(startDate, endDate, clientId)
                .collectList()
                .flatMapMany(movements -> accountStatusService.getAccountStatus(movements, clientId));

    }


}