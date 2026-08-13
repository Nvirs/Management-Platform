export function handleRegistrationConfirmed(payload) {
  const { registrationId, eventId, userEmail, confirmedAt } = payload;

  console.log(
    `[notification] confirmation email -> ${userEmail} ` +
      `(registrationId=${registrationId}, eventId=${eventId}, confirmedAt=${confirmedAt})`
  );
}
