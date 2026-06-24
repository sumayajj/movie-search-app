# Movie Search App

A JavaFX desktop application that searches The Movie Database (TMDB) API for movies, displaying posters, ratings, and an in-app detail view with cast information.

## Features

- Live search powered by the TMDB API
- Displays popular movies on launch
- Poster grid with movie/rating badges
- Click any movie for a detail view with overview, genres, and cast (circular profile photos)
- In-app navigation with a back button (no popups or new windows)
- Dark, Netflix-inspired theme

## Technologies

- Java 17, JavaFX, Maven
- `org.json` for parsing API responses
- Java's built-in `HttpClient` for network requests
- Background threading to keep the UI responsive during API calls

## Setup

This project requires a free TMDB API key to run.

1. Create a free account at [themoviedb.org](https://www.themoviedb.org/signup)
2. Request a free Developer API key at Settings → API
3. Set it as an environment variable on your machine:
