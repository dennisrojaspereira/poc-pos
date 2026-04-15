create table transactions (
    transaction_id varchar(64) primary key,
    terminal_id varchar(64) not null,
    nsu varchar(64) not null,
    amount numeric(19, 2) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index uk_transactions_terminal_nsu
    on transactions (terminal_id, nsu);
