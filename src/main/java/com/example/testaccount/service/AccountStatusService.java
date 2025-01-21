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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountStatusService {

    private final TestApi testApi;
    private final AccountService accountService;
    private final AccountTypeService accountTypeService;


    public Flux<AccountStatusDto> getAccountStatus(List<Movement> movements, Integer clientId) {
        return testApi.getClienteById(clientId)
                .flatMapMany(client -> {
                    Map<Integer, List<Movement>> groupedMovements = movements.stream()
                            .collect(Collectors.groupingBy(Movement::getAccountMovementId));

                    return Flux.fromIterable(groupedMovements.entrySet())
                            .flatMap(entry -> {
                                Integer accountMovementId = entry.getKey();
                                List<Movement> movementList = entry.getValue();

                                return accountService.getAccountById(accountMovementId)
                                        .flatMapMany(account -> accountTypeService.getAccountTypeById(account.getAccountTypeId())
                                                .flatMapMany(accountType -> Flux.fromIterable(movementList)
                                                        .map(movement -> {
                                                            BigDecimal previousBalance = movementList.indexOf(movement) == 0 ? account.getInitialBalance() : movementList.get(movementList.indexOf(movement) - 1).getMovementBalance();
                                                            return AccountStatusDto.builder()
                                                                    .date(movement.getMovementDate())
                                                                    .clientName(client.getPersonName())
                                                                    .accountNumber(account.getAccountNumber())
                                                                    .accountType(accountType.getAccountTypeDescription())
                                                                    .initialBalance(previousBalance)
                                                                    .movement(movement.getMovementValue())
                                                                    .finalBalance(movement.getMovementBalance())
                                                                    .build();
                                                        })
                                                )
                                        );
                            });
                });
    }


}