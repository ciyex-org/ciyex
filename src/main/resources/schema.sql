CREATE DATABASE IF NOT EXISTS ciyexdb;

create table ciyexdb.orgs
(
    id          bigint auto_increment
        primary key,
    address     varchar(255) null,
    city        varchar(255) null,
    country     varchar(255) null,
    org_name    varchar(255) not null,
    postal_code varchar(255) null,
    state       varchar(255) null,
    constraint UKl72x9w0lb26ikpybtt5niys03
        unique (org_name)
);

create table ciyexdb.facilities
(
    id            bigint auto_increment
        primary key,
    facility_name varchar(255) not null,
    org_id        bigint       not null,
    constraint FKi34mvosb8ur2rqp1xqnl0mnb1
        foreign key (org_id) references ciyexdb.orgs (id)
);

create table ciyexdb.users
(
    id                bigint auto_increment
        primary key,
    city              varchar(255) null,
    country           varchar(255) null,
    date_of_birth     date         null,
    email             varchar(255) not null,
    full_name         varchar(255) null,
    password          varchar(255) not null,
    phone_number      varchar(255) null,
    postal_code       varchar(255) null,
    profile_image     varchar(255) null,
    security_answer   varchar(255) null,
    security_question varchar(255) null,
    state             varchar(255) null,
    street            varchar(255) null,
    constraint UK6dotkott2kjsp8vw4d0m25fb7
        unique (email)
);

create table ciyexdb.user_facility_roles
(
    id          bigint auto_increment
        primary key,
    role        enum ('ADMIN', 'DOCTOR', 'NURSE', 'PATIENT', 'RECEPTIONIST', 'SUPER_ADMIN') not null,
    facility_id bigint                                                                      not null,
    user_id     bigint                                                                      not null,
    constraint FK4e8r49gfya1hgq99cmllk5tk0
        foreign key (user_id) references ciyexdb.users (id),
    constraint FKrjtg63levu1rirtg993ajjgni
        foreign key (facility_id) references ciyexdb.facilities (id)
);

