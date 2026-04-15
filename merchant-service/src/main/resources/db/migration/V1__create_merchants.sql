create table merchants (
    merchant_id varchar(64) primary key,
    legal_name varchar(255) not null,
    document_number varchar(32) not null unique,
    status varchar(32) not null
);
