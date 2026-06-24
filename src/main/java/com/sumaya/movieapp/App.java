package com.sumaya.movieapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App extends Application {

    private final String apiKey = System.getenv("TMDB_API_KEY");
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w200";
    private static final String PROFILE_BASE_URL = "https://image.tmdb.org/t/p/w185";

    private StackPane screenContainer;
    private VBox browseScreen;
    private FlowPane resultsPane;

    @Override
    public void start(Stage stage) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search for a movie...");
        searchField.setPrefWidth(400);

        Button searchButton = new Button("Search");

        resultsPane = new FlowPane();
        resultsPane.setHgap(15);
        resultsPane.setVgap(15);
        resultsPane.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(resultsPane);
        scrollPane.setFitToWidth(true);

        searchButton.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                return;
            }
            searchMovies(query, resultsPane);
        });

        searchField.setOnAction(e -> searchButton.fire());

        HBox searchRow = new HBox(10, searchField, searchButton);
        searchRow.setPadding(new Insets(15));
        searchRow.setAlignment(Pos.CENTER_LEFT);

        browseScreen = new VBox(10, searchRow, scrollPane);
        browseScreen.setStyle("-fx-background-color: #141414;");

        screenContainer = new StackPane(browseScreen);

        Scene scene = new Scene(screenContainer, 750, 600);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Movie Search");
        stage.show();

        loadPopularMovies(resultsPane);
    }

    private void loadPopularMovies(FlowPane resultsPane) {
        new Thread(() -> {
            try {
                String url = "https://api.themoviedb.org/3/movie/popular?api_key=" + apiKey;
                JSONObject json = fetchJson(url);
                JSONArray results = json.getJSONArray("results");

                Platform.runLater(() -> resultsPane.getChildren().clear());

                for (int i = 0; i < results.length(); i++) {
                    addMovieCard(results.getJSONObject(i), resultsPane);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resultsPane.getChildren().clear();
                    resultsPane.getChildren().add(new Label("Error loading popular movies: " + ex.getMessage()));
                });
            }
        }).start();
    }

    private void searchMovies(String query, FlowPane resultsPane) {
        resultsPane.getChildren().clear();
        resultsPane.getChildren().add(new Label("Searching..."));

        new Thread(() -> {
            try {
                String encodedQuery = query.replace(" ", "%20");
                String url = "https://api.themoviedb.org/3/search/movie?api_key=" + apiKey + "&query=" + encodedQuery;
                JSONObject json = fetchJson(url);
                JSONArray results = json.getJSONArray("results");

                Platform.runLater(() -> resultsPane.getChildren().clear());

                if (results.isEmpty()) {
                    Platform.runLater(() -> resultsPane.getChildren().add(new Label("No results found.")));
                    return;
                }

                for (int i = 0; i < results.length(); i++) {
                    addMovieCard(results.getJSONObject(i), resultsPane);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resultsPane.getChildren().clear();
                    resultsPane.getChildren().add(new Label("Error: " + ex.getMessage()));
                });
            }
        }).start();
    }

    private JSONObject fetchJson(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }

    private void addMovieCard(JSONObject movie, FlowPane resultsPane) {
        String title = movie.optString("title", "Unknown title");
        String releaseDate = movie.optString("release_date", "Unknown");
        double rating = movie.optDouble("vote_average", 0.0);
        String posterPath = movie.optString("poster_path", null);
        int movieId = movie.optInt("id", -1);

        ImageView posterView = new ImageView();
        posterView.setFitWidth(140);
        posterView.setFitHeight(200);
        posterView.setPreserveRatio(true);

        if (posterPath != null) {
            posterView.setImage(new Image(IMAGE_BASE_URL + posterPath, true));
        }

        Label typeBadge = new Label("MOVIE");
        typeBadge.getStyleClass().add("badge-type");

        Label ratingBadge = new Label(String.format("★ %.1f", rating));
        ratingBadge.getStyleClass().add("badge-rating");

        BorderPane badgeOverlay = new BorderPane();
        badgeOverlay.setLeft(typeBadge);
        badgeOverlay.setRight(ratingBadge);
        badgeOverlay.setPadding(new Insets(6));
        badgeOverlay.setPrefWidth(140);
        badgeOverlay.setPickOnBounds(false);

        StackPane posterStack = new StackPane(posterView, badgeOverlay);
        posterStack.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("movie-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(140);

        Label detailsLabel = new Label(releaseDate + " · ★ " + rating);
        detailsLabel.getStyleClass().add("movie-details");

        VBox card = new VBox(8, posterStack, titleLabel, detailsLabel);
        card.getStyleClass().add("movie-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(160);
        card.setOnMouseClicked(e -> {
            if (movieId != -1) {
                openDetailScreen(movieId);
            }
        });

        Platform.runLater(() -> resultsPane.getChildren().add(card));
    }

    private void openDetailScreen(int movieId) {
        VBox loadingScreen = new VBox(new Label("Loading..."));
        loadingScreen.setAlignment(Pos.CENTER);
        loadingScreen.setPadding(new Insets(20));

        Platform.runLater(() -> screenContainer.getChildren().add(loadingScreen));

        new Thread(() -> {
            try {
                String url = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + apiKey;
                String creditsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + apiKey;

                JSONObject details = fetchJson(url);
                JSONObject credits = fetchJson(creditsUrl);

                String title = details.optString("title", "Unknown");
                String overview = details.optString("overview", "No description available.");
                String releaseDate = details.optString("release_date", "Unknown");
                double rating = details.optDouble("vote_average", 0.0);
                String posterPath = details.optString("poster_path", null);

                JSONArray genresArray = details.optJSONArray("genres");
                StringBuilder genres = new StringBuilder();
                if (genresArray != null) {
                    for (int i = 0; i < genresArray.length(); i++) {
                        genres.append(genresArray.getJSONObject(i).optString("name", ""));
                        if (i < genresArray.length() - 1) genres.append(", ");
                    }
                }

                JSONArray cast = credits.getJSONArray("cast");
                int castLimit = Math.min(cast.length(), 8);

                Platform.runLater(() -> {
                    VBox detailScreen = buildDetailScreen(title, overview, releaseDate, rating, posterPath, genres.toString(), cast, castLimit, loadingScreen);
                    screenContainer.getChildren().remove(loadingScreen);
                    screenContainer.getChildren().add(detailScreen);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    screenContainer.getChildren().remove(loadingScreen);
                    Label errorLabel = new Label("Error loading details: " + ex.getMessage());
                    VBox errorScreen = new VBox(10, makeBackButton(), errorLabel);
                    errorScreen.setPadding(new Insets(15));
                    screenContainer.getChildren().add(errorScreen);
                });
            }
        }).start();
    }

    private VBox buildDetailScreen(String title, String overview, String releaseDate, double rating,
                                    String posterPath, String genres, JSONArray cast, int castLimit, VBox loadingScreen) {

        Button backButton = makeBackButton();

        ImageView posterView = new ImageView();
        posterView.setFitWidth(180);
        posterView.setFitHeight(260);
        posterView.setPreserveRatio(true);
        if (posterPath != null) {
            posterView.setImage(new Image(IMAGE_BASE_URL + posterPath, true));
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("detail-title");
        titleLabel.setWrapText(true);

        Label metaLabel = new Label(releaseDate + "  ·  ★ " + rating + (genres.isEmpty() ? "" : "  ·  " + genres));
        metaLabel.getStyleClass().add("detail-meta");
        metaLabel.setWrapText(true);

        Label overviewLabel = new Label(overview);
        overviewLabel.getStyleClass().add("detail-overview");
        overviewLabel.setWrapText(true);
        overviewLabel.setMaxWidth(500);

        VBox infoBox = new VBox(10, titleLabel, metaLabel, overviewLabel);
        infoBox.setPadding(new Insets(0, 0, 0, 20));

        HBox topSection = new HBox(posterView, infoBox);
        topSection.setPadding(new Insets(20));

        Label castHeader = new Label("Cast");
        castHeader.getStyleClass().add("detail-title");
        castHeader.setStyle("-fx-font-size: 18px;");

        HBox castRow = new HBox(15);
        for (int i = 0; i < castLimit; i++) {
            JSONObject actor = cast.getJSONObject(i);
            String name = actor.optString("name", "");
            String profilePath = actor.optString("profile_path", null);

            ImageView actorImage = new ImageView();
            actorImage.setFitWidth(70);
            actorImage.setFitHeight(70);
            actorImage.setPreserveRatio(true);

            if (profilePath != null) {
                actorImage.setImage(new Image(PROFILE_BASE_URL + profilePath, true));
            }

            Circle clip = new Circle(35, 35, 35);
            actorImage.setClip(clip);

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("cast-name");
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(80);
            nameLabel.setAlignment(Pos.CENTER);

            VBox actorBox = new VBox(5, actorImage, nameLabel);
            actorBox.setAlignment(Pos.TOP_CENTER);
            actorBox.setPrefWidth(80);

            castRow.getChildren().add(actorBox);
        }

        ScrollPane castScroll = new ScrollPane(castRow);
        castScroll.setFitToHeight(true);
        castScroll.setPrefHeight(150);

        VBox castSection = new VBox(10, castHeader, castScroll);
        castSection.setPadding(new Insets(0, 20, 20, 20));

        ScrollPane detailScroll = new ScrollPane(new VBox(topSection, castSection));
        detailScroll.setFitToWidth(true);

        VBox screen = new VBox(backButton, detailScroll);
        screen.setStyle("-fx-background-color: #141414;");
        screen.setPrefHeight(600);
        screen.setPrefWidth(750);
        return screen;

    }

    private Button makeBackButton() {
        Button backButton = new Button("← Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            screenContainer.getChildren().remove(screenContainer.getChildren().size() - 1);
        });
        return backButton;
    }

    public static void main(String[] args) {
        launch(args);
    }
}