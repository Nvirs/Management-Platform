import assert from 'node:assert/strict';
import { test } from 'node:test';

import { handleRegistrationConfirmed } from '../src/handlers/registrationConfirmed.js';

test('logs a confirmation message with the payload details', () => {
  const logs = [];
  const originalLog = console.log;
  console.log = (msg) => logs.push(msg);

  try {
    handleRegistrationConfirmed({
      registrationId: 'reg-1',
      eventId: 'evt-1',
      userEmail: 'user@example.com',
      confirmedAt: '2026-08-13T00:00:00Z',
    });
  } finally {
    console.log = originalLog;
  }

  assert.equal(logs.length, 1);
  assert.match(logs[0], /user@example\.com/);
  assert.match(logs[0], /reg-1/);
  assert.match(logs[0], /evt-1/);
});
