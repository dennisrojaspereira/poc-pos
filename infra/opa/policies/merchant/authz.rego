package merchant.authz

default allow := false

allow if {
    input.method == "GET"
    role_allowed_for_read
}

allow if {
    input.method == "POST"
    "admin" in input.roles
}

role_allowed_for_read if {
    some role in input.roles
    role == "admin" or role == "operator" or role == "auditor"
}
