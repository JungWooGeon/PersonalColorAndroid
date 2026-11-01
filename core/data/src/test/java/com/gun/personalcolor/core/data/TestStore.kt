package com.gun.personalcolor.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.test.TestScope
import org.junit.rules.TemporaryFolder

/** A real Preferences DataStore on a temp file, living in the test's background scope. */
fun TestScope.testStore(tmp: TemporaryFolder): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { tmp.newFile("x.preferences_pb") })
