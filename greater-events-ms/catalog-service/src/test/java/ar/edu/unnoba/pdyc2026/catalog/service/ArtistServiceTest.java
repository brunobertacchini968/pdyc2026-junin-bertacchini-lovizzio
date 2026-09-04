package ar.edu.unnoba.pdyc2026.catalog.service;

import ar.edu.unnoba.pdyc2026.catalog.exception.BusinessException;
import ar.edu.unnoba.pdyc2026.catalog.exception.ResourceNotFoundException;
import ar.edu.unnoba.pdyc2026.catalog.model.Artist;
import ar.edu.unnoba.pdyc2026.catalog.model.Event;
import ar.edu.unnoba.pdyc2026.catalog.model.Genre;
import ar.edu.unnoba.pdyc2026.catalog.repository.ArtistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ArtistServiceImpl artistService;

    @Test
    @DisplayName("Debe crear un artista válidamente")
    void testCreateArtistSuccess() {
        Artist artist = new Artist("Fito Paez", Genre.ROCK);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);

        Artist created = artistService.create("Fito Paez", Genre.ROCK);

        assertNotNull(created);
        assertEquals("Fito Paez", created.getName());
        assertEquals(Genre.ROCK, created.getGenre());
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear un artista con nombre nulo o vacío")
    void testCreateArtistInvalidName() {
        assertThrows(BusinessException.class, () -> artistService.create(null, Genre.ROCK));
        assertThrows(BusinessException.class, () -> artistService.create("   ", Genre.ROCK));
        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear un artista con género nulo")
    void testCreateArtistNullGenre() {
        assertThrows(BusinessException.class, () -> artistService.create("Fito Paez", null));
        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe buscar artista por id o lanzar ResourceNotFoundException")
    void testFindById() {
        Artist artist = new Artist("Charly Garcia", Genre.ROCK);
        artist.setId(1L);
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistRepository.findById(2L)).thenReturn(Optional.empty());

        Artist found = artistService.findById(1L);
        assertEquals("Charly Garcia", found.getName());

        assertThrows(ResourceNotFoundException.class, () -> artistService.findById(2L));
    }

    @Test
    @DisplayName("Debe listar artistas filtrando por género o retornando todos")
    void testFindAll() {
        Artist rockArtist = new Artist("Charly", Genre.ROCK);
        Artist jazzArtist = new Artist("Miles", Genre.JAZZ);

        when(artistRepository.findAll()).thenReturn(List.of(rockArtist, jazzArtist));
        when(artistRepository.findByGenre(Genre.ROCK)).thenReturn(List.of(rockArtist));

        assertEquals(2, artistService.findAll(null).size());
        List<Artist> rocks = artistService.findAll(Genre.ROCK);
        assertEquals(1, rocks.size());
        assertEquals(Genre.ROCK, rocks.get(0).getGenre());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar un artista que ya tiene eventos asociados")
    void testUpdateArtistWithEventsFails() {
        Artist artist = new Artist("Divididos", Genre.ROCK);
        artist.setId(1L);
        artist.setEvents(Set.of(new Event("Recital", "Desc", LocalDate.now().plusDays(10))));

        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThrows(BusinessException.class, () -> artistService.update(1L, "Divididos Nuevo", Genre.ROCK));
        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar sin enviar cambios válidos")
    void testUpdateArtistWithoutChanges() {
        Artist artist = new Artist("Spinetta", Genre.ROCK);
        artist.setId(1L);
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        assertThrows(BusinessException.class, () -> artistService.update(1L, null, null));
        assertThrows(BusinessException.class, () -> artistService.update(1L, "   ", null));
    }

    @Test
    @DisplayName("Debe eliminar físicamente el artista si no tiene eventos")
    void testDeleteArtistWithoutEvents() {
        Artist artist = new Artist("Cerati", Genre.ROCK);
        artist.setId(1L);
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        artistService.delete(1L);

        verify(artistRepository).delete(artist);
        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe desactivar lógicamente el artista si tiene eventos asignados")
    void testDeleteArtistWithEventsSoftDeletes() {
        Artist artist = new Artist("Cerati", Genre.ROCK);
        artist.setId(1L);
        artist.setEvents(Set.of(new Event("Cosquin Rock", "Festival", LocalDate.now().plusDays(5))));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        artistService.delete(1L);

        assertFalse(artist.isActive(), "El artista debe marcarse como inactivo");
        verify(artistRepository).save(artist);
        verify(artistRepository, never()).delete(any());
    }
}
