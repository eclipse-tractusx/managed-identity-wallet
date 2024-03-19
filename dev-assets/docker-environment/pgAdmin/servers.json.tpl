{
    "Servers": {
        "1": {
            "Name": "Local",
            "Group": "Servers",
            "Host": "postgres",
            "Port": 5432,
            "MaintenanceDB": "postgres",
            "Username": "$POSTGRES_USER",
            "UseSSHTunnel": 0,
            "TunnelPort": "22",
            "TunnelAuthentication": 0,
            "KerberosAuthentication": false,
            "ConnectionParameters": {
                "sslmode": "prefer",
                "connect_timeout": 10,
                "sslcert": "<STORAGE_DIR>/.postgresql/postgresql.crt",
                "sslkey": "<STORAGE_DIR>/.postgresql/postgresql.key"
            }
        }
    }
}
