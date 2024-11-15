﻿using Android.App;
using Android.Content.PM;
using Android.OS;
using Couchbase.Lite.Testing.Maui;

namespace TestServer.Maui;

[Activity(Theme = "@style/Maui.SplashTheme", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.UiMode | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize | ConfigChanges.Density)]
public class MainActivity : MauiAppCompatActivity
{
    protected override void OnCreate(Bundle savedInstanceState)
    {
        base.OnCreate(savedInstanceState);

        Couchbase.Lite.Support.Droid.Activate(ApplicationContext);
        ResolvePath.ApplicationContext = ApplicationContext;
    }
}
