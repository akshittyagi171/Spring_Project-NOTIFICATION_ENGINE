#!/bin/sh
set -e
echo "Seeding Vault secrets..."

vault kv put secret/notification-engine/common \
  DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" REDIS_PASSWORD="$REDIS_PASSWORD"

vault kv put secret/notification-engine/email-consumer \
  SENDGRID_API_KEY="$SENDGRID_API_KEY" SENDGRID_FROM_EMAIL="$SENDGRID_FROM_EMAIL"

vault kv put secret/notification-engine/sms-consumer \
  TWILIO_ACCOUNT_SID="$TWILIO_ACCOUNT_SID" TWILIO_AUTH_TOKEN="$TWILIO_AUTH_TOKEN" TWILIO_FROM_SMS_NUMBER="$TWILIO_FROM_SMS_NUMBER"

vault kv put secret/notification-engine/whatsapp-consumer \
  TWILIO_ACCOUNT_SID="$TWILIO_ACCOUNT_SID" TWILIO_AUTH_TOKEN="$TWILIO_AUTH_TOKEN" TWILIO_FROM_WHATSAPP_NUMBER="$TWILIO_FROM_WHATSAPP_NUMBER"

vault kv put secret/notification-engine/notificationservice \
  TWILIO_AUTH_TOKEN="$TWILIO_AUTH_TOKEN" SENDGRID_EVENT_WEBHOOK_VERIFICATION_KEY="$SENDGRID_EVENT_WEBHOOK_VERIFICATION_KEY"

echo "Done."