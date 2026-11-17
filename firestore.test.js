const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');
const { test, describe, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');

const PROJECT_ID = process.env.GCP_PROJECT || 'demo-no-project';
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085').split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

function validProfile(uid, visible = true) {
  return {
    userId: uid,
    displayName: `Reader ${uid}`,
    focusScore: 240,
    totalFocusMinutes: 120,
    completedSessionsCount: 4,
    currentStreak: 3,
    totalBlockedAttempts: 2,
    treeStage: 3,
    isLeaderboardVisible: visible,
    isPro: false,
    createdAt: new Date(),
    updatedAt: new Date()
  };
}

function validRoom(roomId, hostId) {
  return {
    roomId,
    roomCode: 'READ42',
    name: 'Evening Silent Reading',
    hostId,
    hostName: 'Alice',
    durationMinutes: 25,
    status: 'waiting',
    participantCount: 1,
    isPublic: true,
    createdAt: new Date(),
    updatedAt: new Date()
  };
}

describe('FocusLock Firestore Security Rules', () => {
  before(async () => {
    const rules = fs.readFileSync('./firestore.rules', 'utf8');
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules,
        host: emulatorHost,
        port: emulatorPort
      }
    });
  });

  after(async () => {
    if (testEnv) await testEnv.cleanup();
  });

  beforeEach(async () => {
    if (testEnv) await testEnv.clearFirestore();
  });

  test('Unauthenticated users cannot read or write any document', async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.collection('users').doc('alice').get());
    await assertFails(anonDb.collection('users').doc('alice').set(validProfile('alice')));
    await assertFails(anonDb.collection('focusRooms').doc('room1').get());
  });

  test('Owner can create, update, and delete own profile; immutable fields enforced', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const ref = aliceDb.collection('users').doc('alice');
    await assertSucceeds(ref.set(validProfile('alice')));
    await assertSucceeds(ref.get());
    await assertSucceeds(ref.update({ focusScore: 300, updatedAt: new Date() }));
    await assertFails(ref.update({ userId: 'bob', updatedAt: new Date() }));
    await assertSucceeds(ref.delete());
  });

  test('Private settings & session backups are strictly isolated to owner', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();

    await assertSucceeds(
      aliceDb.collection('users').doc('alice').collection('private').doc('settings').set({
        userId: 'alice',
        isDarkMode: true,
        cloudSyncEnabled: true,
        partnerEmail: 'partner@example.com',
        updatedAt: new Date()
      })
    );
    await assertFails(bobDb.collection('users').doc('alice').collection('private').doc('settings').get());
    await assertFails(
      bobDb.collection('users').doc('alice').collection('private').doc('settings').set({
        userId: 'alice',
        isDarkMode: false,
        cloudSyncEnabled: false,
        updatedAt: new Date()
      })
    );

    await assertSucceeds(
      aliceDb.collection('users').doc('alice').collection('sessions').doc('s1').set({
        userId: 'alice',
        sessionId: 's1',
        timestampMs: 1700000000,
        durationSeconds: 1500,
        blockedAttempts: 2,
        isCompleted: true,
        readingAppPackage: 'com.amazon.kindle',
        createdAt: new Date()
      })
    );
    await assertFails(bobDb.collection('users').doc('alice').collection('sessions').doc('s1').get());
  });

  test('Leaderboard respects isLeaderboardVisible privacy toggle', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();

    await assertSucceeds(aliceDb.collection('users').doc('alice').set(validProfile('alice', true)));
    await assertSucceeds(bobDb.collection('users').doc('bob').set(validProfile('bob', false)));

    await assertSucceeds(bobDb.collection('users').doc('alice').get());
    await assertFails(aliceDb.collection('users').doc('bob').get());
    await assertSucceeds(bobDb.collection('users').where('isLeaderboardVisible', '==', true).get());
  });

  test('Focus rooms enforce host ownership and participant permissions', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();

    // Bob cannot spoof Alice as host
    await assertFails(bobDb.collection('focusRooms').doc('room1').set(validRoom('room1', 'alice')));
    // Alice creates room1
    await assertSucceeds(aliceDb.collection('focusRooms').doc('room1').set(validRoom('room1', 'alice')));

    // Bob can join participants as bob, not as charlie
    await assertFails(
      bobDb.collection('focusRooms').doc('room1').collection('participants').doc('charlie').set({
        userId: 'charlie',
        displayName: 'Charlie',
        status: 'ready',
        distractionAttempts: 0,
        treeStage: 1,
        joinedAt: new Date(),
        updatedAt: new Date()
      })
    );
    await assertSucceeds(
      bobDb.collection('focusRooms').doc('room1').collection('participants').doc('bob').set({
        userId: 'bob',
        displayName: 'Bob',
        status: 'focusing',
        distractionAttempts: 0,
        treeStage: 2,
        joinedAt: new Date(),
        updatedAt: new Date()
      })
    );

    // Bob can update participantCount when joining, NOT rename room or delete room
    await assertSucceeds(
      bobDb.collection('focusRooms').doc('room1').update({
        participantCount: 2,
        updatedAt: new Date()
      })
    );
    await assertFails(
      bobDb.collection('focusRooms').doc('room1').update({
        name: 'Hacked Room',
        updatedAt: new Date()
      })
    );
    await assertFails(bobDb.collection('focusRooms').doc('room1').delete());

    // Alice (host) can update status and delete room
    await assertSucceeds(
      aliceDb.collection('focusRooms').doc('room1').update({
        status: 'active',
        updatedAt: new Date()
      })
    );
    await assertSucceeds(aliceDb.collection('focusRooms').doc('room1').delete());
  });
});
