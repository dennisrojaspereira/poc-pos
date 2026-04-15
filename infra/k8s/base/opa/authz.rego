package merchant.authz

default allow = false

allow {
    input.method == "GET"
    role_allowed_for_read
}

allow {
    input.method == "POST"
    some i
    input.roles[i] == "admin"
}

role_allowed_for_read {
    some i
    input.roles[i] == "admin"
}

role_allowed_for_read {
    some i
    input.roles[i] == "operator"
}

role_allowed_for_read {
    some i
    input.roles[i] == "auditor"
}
