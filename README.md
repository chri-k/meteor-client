
<p align="center">
<img src="https://meteorclient.com/icon.png" alt="meteor-client-logo" width="15%"/>
</p>

<h1 align="center">Meteor</h1>
<p align="center">A Minecraft Fabric Utility Mod for anarchy servers.</p>

## LTS

This fork of meteor backports changes from newer versions to the 1.21.4 version of minecraft, for compatibility with some add-ons
and other client mods such as RusherHack and Future.

This fork also includes some unmerged PRs of meteor, adding some features not present in the official version.

Not all changes are ported, and some add-ons might break as a result of changes.

If reporting any issues on discord while using this, direct the report specifically to @31a05b9c, as this is not an official version.

## Usage

### Building and Installation
- Clone this repository
- Run `./gradlew build`
- Add resulting `./build/libs/meteor-client-*-sww-lts-for-1.21.4.jar` to your `mods` directory

## Contributions, Bugs, and Suggestions

Only open PRs to fixes to bugs specifically caused by this fork.

Everything else should go to upstream meteor.

## Credits

[Cabaletta](https://github.com/cabaletta) and [WagYourTail](https://github.com/wagyourtail) for [Baritone](https://github.com/cabaletta/baritone)  
The [Fabric Team](https://github.com/FabricMC) for [Fabric](https://github.com/FabricMC/fabric-loader) and [Yarn](https://github.com/FabricMC/yarn)

## Licensing
This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). 

If you use **ANY** code from the source:
- You must disclose the source code of your modified work and the source code you took from this project. This means you are not allowed to use code from this project (even partially) in a closed-source and/or obfuscated application.
- You must state clearly and obviously to all end users that you are using code from this project.
- Your application must also be licensed under the same license.

*If you have any other questions, check meteor's [FAQ](https://meteorclient.com/faq) or ask in meteor's [Discord](https://meteorclient.com/discord) server.*
