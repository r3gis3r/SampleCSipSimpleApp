Introduction
============

This project aim to show very basic way to include or integrate CSipSimple in another application

Before doing everything, please read with a lot of attention :
[Licensing information](http://code.google.com/p/csipsimple/wiki/Licensing?wl=en)

As you will notice there is several ways to integrate with CSipSimple. Depending on the way you choose you will have to adapt manifest and included libs.

The standalone way
------------------

### Info
This implies that you have a Commercial License of pjsip and that you respect LGPL license of CSipSimple.

### Setup project
* Checkout and be able to build CSipSimple. For that read the [How To Build wiki page](http://code.google.com/p/csipsimple/wiki/HowToBuild?wl=en). Once you are sure that you can build CSipSimple and run it by your own means, you can proceed the next step.
* Turn CSipSimple into a library project. For that read [official android doc about library projects](http://developer.android.com/guide/developing/projects/projects-eclipse.html)
* Mute some conflicting CSipSimple definitions of permissions in manifest of CSipSimple. This is marked as `<!-- COMMENT THIS if you plan to use in library mode -->` In the current trunk version.
* Then you can checkout the project of this github and reference CSipSimple as a library (refer to official android doc about libraries).

Keep in mind that obviously in this case you can't install both your app and CSipSimple. It will conflicts due to the fact content providers names are indentical.

The plugin way
--------------

### Info
If you plan to be a CSipSimple plugin or want to distribute two apps (one that is CSipSimple repackaged as a plugin and the other yours), this project is still valid but it's slightly different to setup.
In this case the repackaged CSipSimple (or CSipSimple itself) remains under GPL license terms. And your plugin can be released under your license terms.

### Setup project

* Checkout this project.
* Checkout CSipSimple api part (this is a package that is standalone and integrated in CSipSimple source code).
* Checkout CSipSimple strings api part (this is a folder containing useful string).

To do so :

    cd SampleCSipSimpleApp/src/
    mkdir -p com/csipsimple
    cd com/csipsimple
    svn checkout http://csipsimple.googlecode.com/svn/trunk/CSipSimple/src/com/csipsimple/api api
    cd ../../..
    mkdir -p res/values
    cd res/values
    svn checkout http://csipsimple.googlecode.com/svn/trunk/CSipSimple/res/values/api_strings.xml api_strings.xml

Then you have to change manifest to be in plugin mode. So no need to redefine all entry points of CSipSimple because all are already defined in CSipSimple app part.

* Remove the reference to the CSipSimple library in eclipse settings of the project
* Delete the `AndroidManifest.xml` file and rename `AndroidManifestPlugin.xml` file to `AndroidManifest.xml`.

