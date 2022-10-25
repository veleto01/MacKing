package macking;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MacKing {

    private final int mesasTotales;
    int numEmpleados;
    int mesaALimpiar = -1;
    private volatile int mesasLibres;
    private final boolean[] mesasSucias;
    private final boolean[] mesasLimpas;
    private boolean finDeJornada;
    private int contadorPedidos;
    private final Lock DarComidaLock = new ReentrantLock();
    private final Lock ControlDeMesas = new ReentrantLock();
	private final Lock ContadorPedidos = new ReentrantLock();
    private final Semaphore colaDeClientes = new Semaphore(0);
    public final Semaphore ControlEmpleados = new Semaphore(0);
    private final Semaphore entrarEmpleado = new Semaphore(0);
    public final Semaphore EsperarClientePedido = new Semaphore(1);
    private final Semaphore hacerColaCogerMesa;
    private final BlockingQueue<Ticket> colaTicket = new LinkedBlockingQueue<Ticket>();
    private final BlockingQueue<Pedido> PedidosCliente = new LinkedBlockingQueue<Pedido>();
    private final BlockingQueue<Pedido> ColaPedidos = new LinkedBlockingQueue<Pedido>();
	private CountDownLatch esperarEmpleados;
    private CountDownLatch esperandoAMesasLimpias;
    //private CountDownLatch vaciarRestaurante;
    private CountDownLatch abrirAClientes;
    private ConcurrentLinkedQueue<Thread> listaDeEmpleados;
    private final Exchanger<Comida> ComidaExchanger = new Exchanger<Comida>();
    private final Exchanger<Double> importeExchanger = new Exchanger<Double>();
    private final Exchanger<Double> Comprobar = new Exchanger<Double>();
    private List<LinkedBlockingQueue<Producto>> productosElaborados;


    public MacKing(int numMesas) {
        mesasTotales = numMesas;
        mesasSucias = new boolean[numMesas];
        mesasLibres = numMesas;
        mesasLimpas = new boolean[numMesas];
        contadorPedidos = 1;
        esperandoAMesasLimpias = new CountDownLatch(0);
        abrirAClientes = new CountDownLatch(1);
        //vaciarRestaurante = new CountDownLatch(0);
        listaDeEmpleados = new ConcurrentLinkedQueue<Thread>();
        hacerColaCogerMesa = new Semaphore(numMesas, true);
        for (int i = 0; i < numMesas; i++) {
            mesasLimpas[i] = true;
            mesasSucias[i] = false;
        }
    }

    public void entrarComoCliente() {
        try {
            abrirAClientes.await();
            System.out.println("Hola soy :" + Thread.currentThread().getName() + " y voy a entrar al restaurante");
        } catch (InterruptedException e) {
            System.err.println("Hola soy :" + Thread.currentThread().getName() + " y no he podido entrar por :" + e);
        }
    }

    public void hacerColaSiHayGente() {
        try {
            colaDeClientes.acquire();
        } catch (InterruptedException e) {
            System.err.println("Hola soy :" + Thread.currentThread().getName() + " y no he podido entrar a la cola por:" + e);
        }
    }

    public void hacerPedido(Pedido pedido) {
        try {
            EsperarClientePedido.acquire();
            contadorPedidos++;
            PedidosCliente.put(pedido);
        } catch (InterruptedException e) {
        }
    }

    public double esperarImporte(double importe) throws InterruptedException {
        double precioComida = importeExchanger.exchange(null);
        double dinero = importe-precioComida;
        Comprobar.exchange(dinero);
        EsperarClientePedido.release();
        return dinero;
    }

    public Ticket pagarYRecogerTicket(Pedido pedido) {
        Ticket ticket = new Ticket(pedido);
        colaDeClientes.release();
        return ticket;
    }

    public Comida esperarPedido(Ticket ticket) {
        Comida comida = null;
        try {
            colaTicket.put(ticket);
            DarComidaLock.lock();
            comida = ComidaExchanger.exchange(null);
            DarComidaLock.unlock();
        } catch (InterruptedException e) {
        }
        return comida;
    }

    public void sentarseEnMesa() {
        try {
            hacerColaCogerMesa.acquire();
        } catch (InterruptedException e) {
        }
        ControlDeMesas.lock();
        for (int i = 0; i < mesasLimpas.length; i++) {
            if (mesasLimpas[i]) {
                mesasLimpas[i] = false;
                mesasLibres--;
                break;
            }
        }
        ControlDeMesas.unlock();
    }

    public void dejarMesa() {
        ControlDeMesas.lock();
        for (int i = 0; i < mesasLimpas.length; i++) {
            if (mesasLimpas[i]) {
                mesasSucias[i] = true;
                break;
            }
        }
        ControlDeMesas.unlock();

    }

    public void abrirParaEmpleados() {
		finDeJornada = false;
		ContadorPedidos.lock();
		contadorPedidos = 0;
		ContadorPedidos.unlock();
		esperarEmpleados = new CountDownLatch(numEmpleados);
		entrarEmpleado.release(numEmpleados);
    }

    public void abrirParaClientes() {
		try {
			esperarEmpleados.await();
			System.out.println("Llegaron todos los empleados");
			abrirAClientes.countDown();
		} catch (InterruptedException e) {
			System.err.println(Thread.currentThread().getName() + e);
		}
    }

    public Integer getNumClientesCola() {
		return (colaDeClientes.getQueueLength() + hacerColaCogerMesa.getQueueLength());
    }

    public void echarElCierre() {
        try {
            while(!listaDeEmpleados.isEmpty()) listaDeEmpleados.remove().interrupt();
            esperarEmpleados.await();
            for (boolean mesasSucia : mesasSucias) {
                System.out.println(mesasSucia);
            }
        } catch (InterruptedException e) {
            System.err.println("Error al esperar a que los empleados terminen");
        }
    }

    public void cerrar() {
        try{
            entrarEmpleado.acquire(numEmpleados);
            ControlEmpleados.release(numEmpleados);
        } catch (InterruptedException e) {
        }
    }
    public int buscarMesaConComida() {
        ControlDeMesas.lock();
        for (int i = 0; i < mesasSucias.length; i++) {
            if(mesasSucias[i]) {
                mesaALimpiar = i;
                break;
            }
        }
        ControlDeMesas.unlock();
        return mesaALimpiar;
    }

    public boolean esFinJornada() {
        return finDeJornada;
    }

    public void entrarComoEmpleado() {
        try {
            entrarEmpleado.acquire();
            listaDeEmpleados.add(Thread.currentThread());
            esperarEmpleados.countDown();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    public Pedido esperarPedidoCliente()throws InterruptedException {
        Pedido aPedido = PedidosCliente.take();
        return aPedido;
    }

    public boolean indicarImporteYCobrar(double importe) throws InterruptedException {
        boolean pagar = true;
        importeExchanger.exchange(importe);
        Double cobrar = Comprobar.exchange(null);
        if (cobrar<0){
            pagar = false;
        }
        return pagar;
    }

    public void tramitarPedido(Pedido pedido) throws InterruptedException {
        ColaPedidos.put(pedido);
    }

    public void salir() {
        entrarEmpleado.release();
        try{
        ControlEmpleados.acquire();
        }catch (InterruptedException e){};

    }

    public Pedido esperarPedidoParaElaborar() throws InterruptedException {
        Pedido pedido = ColaPedidos.take();
        return pedido;
    }

    public void dejarProductoEnCarriles(Producto producto) {
        switch (producto.productoPedido) {
            case COCA_COLA:
                productosElaborados.get(0).add(producto);
                break;
            case NESTEA:
                productosElaborados.get(1).add(producto);
                break;
            case PATATAS_FRITAS:
                productosElaborados.get(2).add(producto);
            case HELADO:
                productosElaborados.get(3).add(producto);
            case WHOPPER:
                productosElaborados.get(4).add(producto);
            case BIGMAC:
                productosElaborados.get(5).add(producto);
            case NUGGETS:
                productosElaborados.get(6).add(producto);
            case PIZZA:
                productosElaborados.get(7).add(producto);
            default:
                break;
        }
    }

    public Ticket esperarClienteConTicket() throws InterruptedException {
        Ticket ticket = colaTicket.take();
        return ticket;
    }

    public Producto cogerProductoDeCarriles(ProductoPedido productoPedido) {
        Producto producto = new Producto(null);
        switch (productoPedido) {
            case COCA_COLA:
                try {
                    producto = productosElaborados.get(0).take();
                } catch (InterruptedException e) {
                }
                break;
            case NESTEA:
                try {
                    producto = productosElaborados.get(1).take();
                } catch (InterruptedException e) {
                }
                break;
            case PATATAS_FRITAS:
                try {
                    producto = productosElaborados.get(2).take();
                } catch (InterruptedException e) {
                }
                break;
            case HELADO:
                try {
                    producto = productosElaborados.get(3).take();
                } catch (InterruptedException e) {
                }
                break;
            case WHOPPER:
                try {
                    producto = productosElaborados.get(4).take();
                } catch (InterruptedException e) {
                }
                break;
            case BIGMAC:
                try {
                    producto = productosElaborados.get(5).take();
                } catch (InterruptedException e) {
                }
                break;
            case NUGGETS:
                try {
                    producto = productosElaborados.get(6).take();
                } catch (InterruptedException e) {
                }
                break;
            case PIZZA:
                try {
                    producto = productosElaborados.get(7).take();
                } catch (InterruptedException e) {
                }
                break;

        }
        return producto;
    }

    public void darComidaACliente(Comida comida) throws InterruptedException {
        DarComidaLock.lock();
        ComidaExchanger.exchange(comida);
        DarComidaLock.unlock();
    }

    public Integer getMesasLibres() {
      return mesasLibres;
    }
    public void estaLimpia(int mesa) {
        mesasSucias[mesa] = false;
        mesasLimpas[mesa] = true;
        esperandoAMesasLimpias.countDown();
        hacerColaCogerMesa.release();
    }
}
