package damas.dao;

import modelo.*;
import damas.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class PartidasDAOImpl implements PartidasDAO {

	@Override
	public ArrayList<MovimientosPartida> devolverPartidasMiTurnoBD(int idUsuario) {
		//devolver un array de movimientos el gestor se encarga de comprobarlos y reproducirlos
		return obtenerMovimientosPartidas(idUsuario, true);
	}

	@Override
	public ArrayList<MovimientosPartida> devolverPartidasNoMiTurnoBD(int idUsuario) {
		//devolver un array de movimientos el gestor se encarga de comprobarlos y reproducirlos
		return obtenerMovimientosPartidas(idUsuario, false);
	}

	@Override
	public ArrayList<MovimientosPartida> devolverPartidasTerminadasBD(int idUsuario) {
		//devolver un array de movimientos el gestor se encarga de comprobarlos y crear
		//un tablero con cada movimiento
		return obtenerMovimientosPartidas(idUsuario);
	}

	@Override
	public int crearPartidaBD(int idUsuarioDesafiado, int idUsuarioDesafiador, int tamanio) {
		// esto devolverá el id de la partida
		int idPartidaCreada = -1;
		String insertarPartidasSQL     = "INSERT INTO partidas (tamanio, finalizada) VALUES (?, ?)";
		String insertUsuarioPartidaSQL = "INSERT INTO usuarios_partidas (id_usuario, id_partida, color) VALUES (?, ?, ?)";
		String obtenerUltimoIdPartida  = "SELECT id_partida FROM partidas order by id_partida DESC limit 1;";

		try (Connection connection = ConexionBD.obtenerConexion()){
			// Insertar la nueva partida
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertarPartidasSQL)) {
				preparedStatement.setInt(1, tamanio);
				preparedStatement.setInt(2, 0);
				preparedStatement.executeUpdate();
			}

			// Obtener el ID de la última partida insertada
			try (PreparedStatement preparedStatement = connection.prepareStatement(obtenerUltimoIdPartida);
				 ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					idPartidaCreada = resultSet.getInt("id_partida");
				}
			}

			// Insertar al usuario desafiado como Blanco
			// Este usuario tiene el primer turno
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertUsuarioPartidaSQL)) {
				preparedStatement.setInt(1, idUsuarioDesafiado);
				preparedStatement.setInt(2, idPartidaCreada);
				preparedStatement.setString(3, "B");
				preparedStatement.executeUpdate();
			}

			// Insertar al usuario desafiador como Negro
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertUsuarioPartidaSQL)) {
				preparedStatement.setInt(1, idUsuarioDesafiador);
				preparedStatement.setInt(2, idPartidaCreada);
				preparedStatement.setString(3, "N");
				preparedStatement.executeUpdate();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return idPartidaCreada;
	}

	@Override
	public void rendirseEnPartidaBD(int idPartida) {
		String sql = "UPDATE partidas SET finalizada = 1 WHERE id_partida = ?";

		try (Connection connection = ConexionBD.obtenerConexion();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, idPartida);
			int rowsUpdated = preparedStatement.executeUpdate();

			if (rowsUpdated == 1) {
				System.out.println("La partida con ID " + idPartida + " ha sido finalizada.");
			} else {
				System.out.println("No se encontró la partida con ID " + idPartida + ".");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void insertarMovimientoBD(int idPartida, int idUsuario, int posXini, int posYini, int posXfin, int posYfin) {

		String sql = "INSERT INTO movimientos (id_usuario, id_partida, pos_ini_x, pos_ini_y, pos_fin_x, pos_fin_y) VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setInt(1, idUsuario);
			preparedStatement.setInt(2, idPartida);
			preparedStatement.setInt(3, posXini);
			preparedStatement.setInt(4, posYini);
			preparedStatement.setInt(5, posXfin);
			preparedStatement.setInt(6, posYfin);

			int rowsAffected = preparedStatement.executeUpdate();

			if (rowsAffected == 1) {
				// La inserción fue exitosa
				System.out.println("Operación de inserción exitosa.");
				connection.commit();
			} else {
				// La inserción no tuvo efecto (ninguna fila afectada)
				System.out.println("Error en la operación de inserción.");
			}

		} catch (SQLException e) {
			System.err.println("Error: " + e);
		}

	}

	@Override
	public boolean comprobarTurno(int idPartida, int idUsuario) {

		String sql = "SELECT id_usuario FROM movimientos WHERE id_partida = ? order by id_movimiento DESC LIMIT 1";

		try (Connection connection = ConexionBD.obtenerConexion();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

				preparedStatement.setInt(1, idPartida);

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						int ultimaJugada = resultSet.getInt("id_usuario");

						// Lógica para determinar si es su turno
                        return ultimaJugada != idUsuario;
					}

				} catch (SQLException e) {
					System.err.println("Error: " + e);
				}

		} catch (SQLException e) {
			System.err.println("Error: " + e);

		}

		return false;
	}

	private ArrayList<MovimientosPartida> obtenerMovimientosPartidas(int idUsuario) {
		ArrayList<MovimientosPartida> movimientosTodasPartidas = new ArrayList<>();
		//Sentencia para obtener los ides de las partidas que no están terminadas
		String sql = "SELECT id_partida FROM usuarios_partidas WHERE id_usuario = ? AND id_partida NOT IN (SELECT id_partida FROM partidas WHERE finalizada = 1)";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setInt(1, idUsuario);
			ResultSet resultado = statement.executeQuery();

			while (resultado.next()) {
				int idPartida = resultado.getInt("id_partida");
				//Recopilar los movimientos de cada partida
				MovimientosPartida movimientosPartida = obtenerMovimientosDeUnaPartida(idPartida);
				movimientosTodasPartidas.add(movimientosPartida);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return movimientosTodasPartidas;
	}

	private ArrayList<MovimientosPartida> obtenerMovimientosPartidas(int idUsuario, boolean suTurno) {
		ArrayList<MovimientosPartida> movimientosTodasPartidas = new ArrayList<>();
		//Sentencia para obtener los ides de las partidas que están terminadas
		String sql = "SELECT id_partida FROM usuarios_partidas WHERE id_usuario = ? AND id_partida NOT IN (SELECT id_partida FROM partidas WHERE finalizada = 1)";

		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement statement = connection.prepareStatement(sql)){
			statement.setInt(1, idUsuario);
			ResultSet resultado = statement.executeQuery();

			while (resultado.next()) {
				int idPartida = resultado.getInt("id_partida");
				//Comprobar si es el turno del jugador pasado
				if (esSuTurno(idPartida, idUsuario) == suTurno) {
					//Recopilar los movimientos de cada partida
					MovimientosPartida movimientosPartida = obtenerMovimientosDeUnaPartida(idPartida);
					movimientosTodasPartidas.add(movimientosPartida);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return movimientosTodasPartidas;
	}

	private boolean esSuTurno(int idPartida, int idUsuario) {
		String sql = "SELECT id_usuario FROM movimientos WHERE id_partida = ? ORDER BY id_movimiento DESC LIMIT 1";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setInt(1, idPartida);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				int idUsuarioUltimoMovimiento = resultSet.getInt("id_usuario");
				return idUsuarioUltimoMovimiento != idUsuario;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;

	}

	private MovimientosPartida obtenerMovimientosDeUnaPartida(int idPartida) {
		MovimientosPartida movimientosPartida = new MovimientosPartida();
		movimientosPartida.setId(idPartida);

		int tamanioPartida = obtenerTamanioPartida(idPartida);
		movimientosPartida.setTamanio(tamanioPartida);

		ArrayList<Movimiento> movimientos = new ArrayList<>();

		String sql = "SELECT * FROM movimientos WHERE id_partida = ? ORDER BY id_movimiento";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setInt(1, idPartida);
			ResultSet rsMovimientos = preparedStatement.executeQuery();
			while (rsMovimientos.next()) {
				Movimiento movimiento = new Movimiento();
				movimiento.setId(rsMovimientos.getInt("id_movimiento"));
				movimiento.setPosIniX(rsMovimientos.getInt("pos_ini_x"));
				movimiento.setPosIniY(rsMovimientos.getInt("pos_ini_y"));
				movimiento.setPosFinX(rsMovimientos.getInt("pos_fin_x"));
				movimiento.setPosFinY(rsMovimientos.getInt("pos_fin_y"));
				movimientos.add(movimiento);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		movimientosPartida.setMovimientos(movimientos);
		return movimientosPartida;
	}

	private int obtenerTamanioPartida(int idPartida) {
		String sql = "SELECT tamanio FROM partidas WHERE id_partida = ?";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, idPartida);
			ResultSet rsTamano = preparedStatement.executeQuery();
			if (rsTamano.next()) {
				return rsTamano.getInt("tamanio");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1; // Valor por defecto si no se encuentra el tamaño de la partida
	}

	@Override
	public ColorPieza comprobarColor(int idPartida, int idUsuario) {
		String sql = "SELECT color FROM usuarios_partidas WHERE id_partida = ? AND id_usuario = ?";
		try (Connection connection = ConexionBD.obtenerConexion();
			 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, idPartida);
			ResultSet rsColor = preparedStatement.executeQuery();
			if (rsColor.next()) {
				return ColorPieza.valueOf(rsColor.getString("color"));

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null; // Valor por defecto si no se encuentra el color del jugador
	}
}
