package com.banking.backend.transfer;

import com.banking.backend.account.Account;
import com.banking.backend.account.AccountRepository;
import com.banking.backend.transaction.Transaction;
import com.banking.backend.transaction.TransactionRepository;
import com.banking.backend.transaction.TransactionStatus;
import com.banking.backend.transaction.TransactionType;
import com.banking.backend.transaction.dto.TransactionDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FxService fxService;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           FxService fxService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fxService = fxService;
    }

    @Transactional
    public TransactionDto transfer(TransferRequest req, String customerNo) {
        if (req.fromAccountNo().equals(req.toAccountNo())) {
            throw new IllegalArgumentException("Source and destination must differ");
        }

        // Lock both accounts in lexical order to avoid deadlocks.
        boolean sourceFirst = req.fromAccountNo().compareTo(req.toAccountNo()) < 0;
        String firstNo = sourceFirst ? req.fromAccountNo() : req.toAccountNo();
        String secondNo = sourceFirst ? req.toAccountNo() : req.fromAccountNo();

        Account first = accountRepository.findByAccountNoForUpdate(firstNo)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + firstNo));
        Account second = accountRepository.findByAccountNoForUpdate(secondNo)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + secondNo));

        Account source = sourceFirst ? first : second;
        Account dest = sourceFirst ? second : first;

        if (!source.getCustomer().getCustomerNo().equals(customerNo)) {
            throw new EntityNotFoundException("Source account not found");
        }

        if (source.getAvailableBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        LocalDateTime now = LocalDateTime.now();
        String refBase = "TRF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // Phase 1 — Reserve
        source.setReservedBalance(source.getReservedBalance().add(req.amount()));
        Transaction debit = newTransaction(source, TransactionType.DEBIT, req.amount(),
                TransactionStatus.PENDING, refBase + "-D", now);
        debit = transactionRepository.save(debit);

        // Phase 2 — Commit
        BigDecimal destAmount = fxService.convert(req.amount(), source.getCurrency(), dest.getCurrency());
        source.setBalance(source.getBalance().subtract(req.amount()));
        source.setReservedBalance(source.getReservedBalance().subtract(req.amount()));
        dest.setBalance(dest.getBalance().add(destAmount));
        debit.setStatus(TransactionStatus.SUCCESS);

        Transaction credit = newTransaction(dest, TransactionType.CREDIT, destAmount,
                TransactionStatus.SUCCESS, refBase + "-C", now);
        transactionRepository.save(credit);

        return TransactionDto.from(debit);
    }

    private Transaction newTransaction(Account account, TransactionType type, BigDecimal amount,
                                       TransactionStatus status, String ref, LocalDateTime date) {
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setStatus(status);
        tx.setTransactionRef(ref);
        tx.setTransactionDate(date);
        return tx;
    }
}
