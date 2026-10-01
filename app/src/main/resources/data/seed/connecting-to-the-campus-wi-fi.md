---
title: "Connecting to the Campus Wi-Fi"
summary: "How to join the iitmwifi network on Android, iOS, macOS, Linux and Windows with your LDAP username and password."
tags: [internet, campus, it]
author: abdirakhim
created: 2026-09-28
updated: 2026-09-28
views: 331
---

The campus Wi-Fi (WiFi) network is called **`iitmwifi`**. You sign in with your **LDAP username and password**:
your IIT Madras login, not a separate Wi-Fi password. Hostel rooms also have Internet and LAN
access. The steps below are from OGE's
[International Student Handbook 2026](https://ge.iitm.ac.in/assets/pdf/international-student-support-handbook.pdf?version=v2),
pages 9 and 22–23.

## Android 12+ and iOS

Select the network `iitmwifi`. In the pop-up, set:

| Setting | Value |
|---|---|
| EAP method | PEAP |
| Phase 2 authentication | MSCHAPV2 |
| CA certificate | *(unspecified)* / No CA certificate required / Do not validate |
| Identity | your LDAP username (for example `username1`) |
| Anonymous identity | your LDAP username |
| Password | your LDAP password |

## macOS and Linux

Connect to `iitmwifi` and enter your LDAP username and password when asked.

## Windows

Windows usually fails if it already has an old `iitmwifi` profile, so remove that first.

1. **Forget the old network:** Network icon (bottom right) → *Network settings* → *Manage Wi-Fi
   settings* → under *Manage known networks*, click `iitmwifi` → *Forget*.
2. *Control Panel* → *Network and Sharing Center* → *Set up a new connection or network*.
3. Choose *Manually connect to a wireless network* → *Next*.
4. Network name: `iitmwifi`. Security type: **WPA2-Enterprise** → *Next*.
5. Click *Change connection settings*.
6. On the *Security* tab, click *Settings* and **untick** *Validating server certificate*. Click
   *Configure…*, **untick** *Automatically use my Windows logon name and password*, then *OK*.
7. Click *Advanced settings*, enable *Specify authentication mode*, choose *User or computer
   authentication*, then *OK*.
8. Connect to `iitmwifi` and enter your LDAP username and password when prompted.

## Before you can do any of this

You need an LDAP account first. Until you have one, use mobile data. See
[[Your First Days on Campus: Onboarding Steps]] for the registration that comes first.
