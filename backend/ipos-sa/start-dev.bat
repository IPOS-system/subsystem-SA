#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# IPOS-SA Quick Start Script (Linux / Mac)
#
# FIRST TIME SETUP:
#   1. Install Java 21:  https://adoptium.net/
#   2. Install MySQL 8:  https://dev.mysql.com/downloads/
#   3. Run the schema:   mysql -u root -p < ../sql/schema.sql
#   4. Edit application-dev.properties — change the MySQL password to yours
#   5. Change this file name from .properties.template to .properties
#   6. Run this script:  ./start-dev.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e

echo "══════════════════════════════════════════"
echo "  IPOS-SA Backend — Starting (dev mode)"
echo "══════════════════════════════════════════"
echo ""

# Check Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed."
    echo "Download Java 21 from: https://adoptium.net/"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java version: $JAVA_VER"

if [ "$JAVA_VER" -lt 21 ] 2>/dev/null; then
    echo "WARNING: Java 21+ is required. You have Java $JAVA_VER."
fi

# Check MySQL is running
if command -v mysqladmin &> /dev/null; then
    if mysqladmin ping -u root --silent 2>/dev/null; then
        echo "MySQL: running"
    else
        echo "WARNING: MySQL may not be running. Start it first."
    fi
fi

echo ""
echo "Starting Spring Boot on port 8080..."
echo "Press Ctrl+C to stop."
echo ""

# Run with dev profile (uses application-dev.properties)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev