package com.example.testaccount.service;

import com.example.testaccount.model.Account;
import com.example.testaccount.model.AccountType;
import com.example.testaccount.repository.AccountRepository;
import com.example.testaccount.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Mono<Void> save (Account account) {
        return accountRepository.save(account).then();
    }

    /**
     * Metodo que se encarga de actualizar un registro de la tabla Account
     * Se considera que el único campo modifcable es el estado
     * @param id
     * @param account
     * @return
     */
    public Mono<Void> update (Integer id, Account account) {
        return accountRepository.findById(id)
                .flatMap(account1 -> {
                    account1.setAccountStatus(account.getAccountStatus());
                    return accountRepository.save(account1);
                }).then();
    }

    public Mono<Account> getAccountById(Integer id) {
        return accountRepository.findById(id);
    }

    public Mono<Void> deleteAccountById(Integer id) {
        return accountRepository.deleteById(id);
    }

    public Flux<Account> getAccounts() {
        return accountRepository.findAll();
    }


}
