package com.example.currentlocation

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import org.junit.Rule
import org.junit.Test

class MainActivityTest{

    @get: Rule
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun testGetLocationButton_expectedCorrectLocation(){
        onView(withId(R.id.get_location_btn)).perform(click())
        onView(withId(R.id.get_location_btn)).perform(click())
        onView(withId(R.id.get_location_btn)).perform(click())
    }
}