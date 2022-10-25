package macking;

import java.util.ArrayList;
import java.util.List;

public class Poblacion {

	private static final int NUM_CLIENTES = 10;
	private static final int NUM_EMP_COGE_PEDIDOS = 5;
	private static final int NUM_EMP_ELABORA_PRODUCTOS = 5;
	private static final int NUM_EMP_PREPARA_PEDIDO = 5;
	private static final int NUM_EMP_LIMPIEZA = 2;
	private static final int NUM_MESAS = 5;

	private static MacKing macKing = new MacKing(NUM_MESAS);

	// ---------------- Métodos procesos -----------------

	protected static void empCogePedidos() {
		while (true) {
			println("Dormir, Jugar al padel...", 500);
			// Esperar hasta poder entrar como empleado
			try{
			macKing.entrarComoEmpleado();
			double caja = 0;
			while (!macKing.esFinJornada()) {
				Pedido pedido;
				pedido = macKing.esperarPedidoCliente();
				double importe = pedido.calcularImporte();
				boolean bool = macKing.indicarImporteYCobrar(importe);
				if(bool) {
					caja += importe;
					println("La caja tiene " + caja + " ?");
					macKing.tramitarPedido(pedido);
				}
				else {
					System.out.println("Pedido cancelado");
				}
			}
			} catch (InterruptedException e){

			}
			macKing.salir();
		}
	}

	protected static void empElaboraProductos() {
		while (true) {
			println("Dormir, Conectarme a facebook...", 1000);
			try {
			macKing.entrarComoEmpleado();
			int numProductos = 0;
			while (!macKing.esFinJornada()) {
				println("A la espera de pedido para elaborar productos");
				Pedido pedido = macKing.esperarPedidoParaElaborar();
				for (ProductoPedido productoPedido : pedido
						.getProductosPedido()) {
					if (productoPedido.esElaborado()) {
						println("Elaborando " + productoPedido
								+ " del pedido nº " + pedido.getNum());
						Producto producto = elaborarProducto(productoPedido,
								pedido);
						macKing.dejarProductoEnCarriles(producto);
						numProductos++;
					}
				}
				println("Ya he elaborado " + numProductos + " productos");
			}
			}catch (InterruptedException e) {
			}
			macKing.salir();
		}
	}

	protected static void empPreparaPedido() {

		while (true) {
			try {
				println("Dormir, tomar cañas", 1000);
				// Esperar hasta poder entrar como empleado

				macKing.entrarComoEmpleado();

				println("Entra al restaurante");
				int numPedidos = 0;
				// Control de fin de jornada
				while (!macKing.esFinJornada()) {
					println("A la espera de un cliente con su ticket");
					Ticket ticket = macKing.esperarClienteConTicket();
					Pedido pedido = ticket.getPedido();
					Comida comida = new Comida();
					for (ProductoPedido productoPedido : pedido
							.getProductosPedido()) {
						Producto producto;
						if (productoPedido.esElaborado()) {
							producto = macKing
									.cogerProductoDeCarriles(productoPedido);
						} else {
							producto = prepararProducto(productoPedido, pedido);
						}
						comida.addProducto(producto);
					}
					// Esperar a que el cliente se lleve la comida para continuar
					macKing.darComidaACliente(comida);
					numPedidos++;
					println("Ya he preparado " + numPedidos + " pedidos");
				}

			} catch (InterruptedException e) {
			}
			macKing.salir();
		}
	}

	protected static void empLimpieza() {
		while (true) {
			println("Dormir, Ir al gimnasio...", 500);
			// Esperar hasta poder entrar como empleado
			macKing.entrarComoEmpleado();
			while (!macKing.esFinJornada()) {
				int numMesa;
				numMesa = macKing.buscarMesaConComida();
				println("Limpiando mesa " + numMesa, 500); // Sin tiempo de limpieza, cuando termina el dia no sale excepcion
				macKing.estaLimpia(numMesa);
			}
			macKing.salir();
		}
	}

	protected static void encargado() {
		while (true) {
			println("Dormir, ver la tele...", 5000);

			// Abre para empleados exclusivamente
			macKing.abrirParaEmpleados();

			println("Organizar el día", 500);

			// Abre para clientes cuando han llegado todos los empleados
			macKing.abrirParaClientes();

			// Supervisa el restaurante 10 veces al día
			List<Integer> numClientesCola = new ArrayList<Integer>();
			List<Integer> numMesasLibres = new ArrayList<Integer>();

			for (int i = 0; i < 10; i++) {
				sleep(300);
				int clientesCola = macKing.getNumClientesCola();
				numClientesCola.add(clientesCola);
				Integer mesasLibres = macKing.getMesasLibres();
				numMesasLibres.add(mesasLibres);
				println("Supervisión: ColaClientes=" + clientesCola
						+ ", MesasLibres=" + mesasLibres);
			}

			// Cuando ha llegado la hora, echa el cierre (para que no entre más
			// clientes)
			// También avisa a los empleados para que terminen
			macKing.echarElCierre();

			// Elabora informes sobre la información de supervisión
			println("Media de clientes en cola: " + media(numClientesCola));
			println("Media de mesas libres: " + media(numMesasLibres));

			// Cuando se han ido todos los clientes y empleados cierra
			macKing.cerrar();

			println("MacKing se cierra");
		}
	}

	protected static void cliente() {
		double dinero = 300;
		while (true) {
			println("Pasear, dormir, trabajar", 1000);
			// El metodo se bloquea hasta que el encargado abre la puerta
			try{
			macKing.entrarComoCliente();
			macKing.hacerColaSiHayGente();

			Pedido pedido = Pedido.crearCualquierPedido();
			macKing.hacerPedido(pedido);
			double importe = macKing.esperarImporte(dinero);
			if (importe > dinero) {
				println("Creo que haré un sinpa....");
				macKing.salir();
				break;
			} else {
				dinero -= importe;
				println("La comida cuesta " + importe);
				println("Me queda " + dinero);
				Ticket ticket = macKing.pagarYRecogerTicket(pedido);
				Comida comida = macKing.esperarPedido(ticket);
				macKing.sentarseEnMesa();
				println("Comiendo " + comida, 500);
				macKing.dejarMesa();
			}
			} catch (InterruptedException e) {
			break;
		}
			macKing.salir();
		}
	}

	// ---------------- Métodos auxiliares -----------------

	private static Producto elaborarProducto(ProductoPedido productoPedido,
			Pedido pedido) {
		println("Elaborando " + productoPedido + " del pedido nº "
				+ pedido.getNum());
		sleepRandom(300);
		return new Producto(productoPedido);
	}

	private static Producto prepararProducto(ProductoPedido productoPedido,
			Pedido pedido) {
		println("Preparando " + productoPedido + " del pedido nº "
				+ pedido.getNum());
		return new Producto(productoPedido);
	}

	private static double media(List<Integer> valores) {
		double media = 0;
		for (Integer num : valores) {
			media += num;
		}
		return media / valores.size();
	}

	private static void println(String mensaje, long sleepMillis) {
		sleepRandom(sleepMillis);
		System.out.println(Thread.currentThread().getName() + ": " + mensaje);
	}

	private static void println(String mensaje) {
		System.out.println(Thread.currentThread().getName() + ": " + mensaje);
	}

	public static void sleepRandom(long millis) {
		sleep((long) (Math.random() * millis));
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// ------------------------- Main -------------------------

	public static void createThreads(int numThreads, String name, Runnable r) {
		for (int i = 0; i < numThreads; i++) {
			new Thread(r, name + "_" + i).start();
		}
	}

	public static void main(String[] args) {

		createThreads(NUM_CLIENTES, "Cliente", () -> cliente());

		createThreads(NUM_EMP_COGE_PEDIDOS, "EmpCogePedidos",
				() -> empCogePedidos());

		createThreads(NUM_EMP_ELABORA_PRODUCTOS, "EmpElaboraProductos",
				() -> empElaboraProductos());

		createThreads(NUM_EMP_PREPARA_PEDIDO, "EmpPreparaPedido",
				() -> empPreparaPedido());

		createThreads(NUM_EMP_LIMPIEZA, "EmpLimpieza", () -> empLimpieza());

		new Thread(() -> encargado(), "Encargado").start();
	}
}
