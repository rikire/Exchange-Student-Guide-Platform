---
id: FEAT-017
title: Rate limits per client address, and the moderator's logout
status: done
covers: [NFR-005]
slice: contribute
routes: ["POST /submissions", "POST /articles/{title}/edits", "POST /contribute/preview", "POST /moderate/login", "POST /moderate/logout"]
tables: []
code:
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/ContributionLimitSettings.java
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/AddressLimit.java
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/ContributionLimits.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/RetryAfter.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/SubmissionController.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/PreviewController.java
  - app/src/main/java/in/ac/iitm/guide/shared/security/FailedLoginLimit.java
  - app/src/main/java/in/ac/iitm/guide/shared/security/ModeratorLoginController.java
  - app/src/main/java/in/ac/iitm/guide/shared/security/WebSecurity.java
  - app/src/main/resources/templates/shared/security/AdminLogin.html
  - app/src/main/resources/templates/shared/security/LogOut.html
  - app/src/main/resources/templates/moderate/ModerationQueue.html
  - app/src/main/resources/templates/moderate/SubmissionReview.html
  - app/src/main/resources/application.yml
tests:
  - app/src/test/java/in/ac/iitm/guide/RateLimitTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/security/ModeratorLoginTest.java
  - app/src/test/java/in/ac/iitm/guide/moderate/ModerationFlowTest.java
---

# FEAT-017 — Rate limits per client address, and the moderator's logout

> Created under the contract the human confirmed on 30 Sep, with the limiters owned by the slices
> that use them (variant C, confirmed the same day). Closes DEBT-011, DEBT-013, DEBT-017 and DEBT-019.

## Why

The submission form, the editor's preview and the moderator login are open to anyone. Until now a
script could fill the moderation queue, keep the server rendering previews, or guess the shared
password as fast as the server answered. The moderator also had no way to end a session on a
computer the office shares.

## Scenario

1. A contributor sends a sixth article within the hour from the same network. The form comes back
   with their text still in it and "Too many submissions from your network. Please try again in N
   minutes." A submission the form refused, such as a taken title, does not count.
2. The editor's preview, asked for more than 120 times in a minute, shows "Too many previews" in
   the pane until the minute has passed. The text being written is untouched.
3. After 10 wrong passwords in 15 minutes from one address, the login answers "Too many failed
   attempts", even to the right password, until the window has passed.
4. The moderator presses "Log out" on the queue or the review page. The login page says "You are
   logged out", and the old session opens nothing.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `POST /submissions`, `POST /articles/{title}/edits` | `429` with the form and the text kept | `contribute/SubmissionForm.html` |
| `POST /contribute/preview` | `429` with a one-paragraph fragment | — |
| `POST /moderate/login` | `429` with the form; the CSRF token replaced on success | `shared/security/AdminLogin.html` |
| `POST /moderate/logout` | Spring Security's logout, then `/moderate/login?logout` | `shared/security/LogOut.html` (the button) |

## Schema impact

None. The counts are held in memory ([ADR-0019](../architecture/adr/ADR-0019-rate-limits-per-client-address.md)).

## Decisions this feature fixed

All confirmed by the human on 30 Sep:

- **The limits:** 5 accepted submissions an hour, new articles and edits together; 120 previews a
  minute; 10 failed logins in 15 minutes. All are settings.
- **The address is the connection's own.** `X-Forwarded-For` is never read.
- **Bucket4j 8.20.0 and Caffeine**, not our own counter.
- **Each owner keeps its own limiter:** `contribute` and the login. Nothing is added to `shared`.
- **Spring Security's own logout**, behind the CSRF token.

## Acceptance criteria

- [x] The sixth submission in an hour answers `429` on the form with the text kept and
      `Retry-After` — `RateLimitTest.the_sixth_submission_in_an_hour_answers_429_on_the_form_with_the_text_kept`
- [x] New articles and edits share one count — `RateLimitTest.new_articles_and_edits_share_one_count`
- [x] A submission refused on the form is not counted — `RateLimitTest.a_submission_refused_on_the_form_is_not_counted`
- [x] Another address has a count of its own — `RateLimitTest.another_address_has_a_count_of_its_own`
- [x] `X-Forwarded-For` does not change whose count it is — `RateLimitTest.a_forwarded_for_header_does_not_change_whose_count_it_is`
- [x] The count starts again once the hour has passed, on a test clock — `RateLimitTest.the_count_starts_again_once_the_hour_has_passed`
- [x] The preview past its limit answers `429` with a fragment — `RateLimitTest.the_preview_past_its_limit_answers_429_with_a_fragment_for_the_pane`
- [x] After ten failed logins even the right password answers `429` — `RateLimitTest.after_ten_failed_logins_even_the_right_password_answers_429`
- [x] Logins with the right password are not counted — `RateLimitTest.logins_with_the_right_password_are_not_counted`
- [x] A limit reached is logged without the address — `RateLimitTest.a_limit_reached_is_logged_as_a_warning_without_the_address`
- [x] Logging out ends the session, and the login page says so — `ModeratorLoginTest.logging_out_ends_the_session_and_the_login_page_says_so`
- [x] A logout without the CSRF token is refused — `ModeratorLoginTest.a_logout_without_the_csrf_token_is_refused_and_the_session_stays`
- [x] Logging in replaces the CSRF token — `ModeratorLoginTest.logging_in_replaces_the_csrf_token_the_browser_held_before`
- [x] The review page offers logging out — `ModerationFlowTest.the_review_page_offers_logging_out`
- [x] Checked by the human on the compose stand

Evidence, 30 Sep: 12 of the 14 tests were red first. Eight limit tests got `200` or `302` where `429`
was expected. The logout, the review page's button and the replaced token failed on the absent
route, button and cookie. Two were green from the start by design: "a refused submission is not
counted" and "right passwords are not counted". They guard against a limit that counts too much,
and would fail if every attempt were counted.

**Accepted by the human on 1 Oct** on the compose stand. On that acceptance NFR-005 is `done`.

## Deliberately out of scope

- The challenge on the submission form ([ADR-0008](../architecture/adr/ADR-0008-abuse-handling-without-accounts.md)).
- Tag visits (FEAT-015): opening a tag page is a `GET` and stays unlimited.
- Counting behind a reverse proxy: the deployment's `server.forward-headers-strategy`, if it has one.

## Open questions

None.
