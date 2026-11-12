import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Smartphone, RefreshCw, XCircle } from 'lucide-react';

export default function MeshSimulator() {
  const [step, setStep] = useState(0);

  // Auto sequence for the animation
  useEffect(() => {
    const timer = setInterval(() => {
      setStep((prev) => (prev + 1) % 5);
    }, 3000);
    return () => clearInterval(timer);
  }, []);

  // Nodes: A(Sender), B(Relay), C(Broken Relay), D(Receiver), E(Alt Relay)
  const nodes = [
    { id: 'A', x: 10, y: 50, label: 'Sender', active: true },
    { id: 'B', x: 35, y: 20, label: 'Relay 1', active: step >= 1 },
    { id: 'C', x: 60, y: 50, label: 'Relay 2', active: step >= 2, broken: step >= 3 },
    { id: 'E', x: 60, y: 75, label: 'Relay 3', active: step >= 4 },
    { id: 'D', x: 90, y: 50, label: 'Receiver', active: step === 2 || step === 4 },
  ];

  const renderPacket = () => {
    if (step === 0) return null; // Wait to send
    
    // Packet coordinates [x, y] in percentages
    let path = [];
    if (step === 1) path = [[10, 50], [35, 20]]; // A -> B
    if (step === 2) path = [[35, 20], [60, 50], [90, 50]]; // B -> C -> D
    if (step === 3) path = [[35, 20], [60, 50]]; // B -> C (Fails)
    if (step === 4) path = [[35, 20], [60, 75], [90, 50]]; // B -> E -> D (Reroute)

    return (
      <motion.div
        key={step}
        initial={{ left: `${path[0][0]}%`, top: `${path[0][1]}%`, opacity: 0 }}
        animate={{ 
          left: path.map(p => `${p[0]}%`), 
          top: path.map(p => `${p[1]}%`), 
          opacity: 1 
        }}
        transition={{ duration: path.length > 2 ? 1.5 : 0.8, ease: "linear" }}
        style={{
          position: 'absolute',
          width: '12px',
          height: '12px',
          background: step === 3 ? 'red' : 'var(--accent-neon)',
          borderRadius: '50%',
          transform: 'translate(-50%, -50%)',
          boxShadow: step === 3 ? '0 0 10px red' : '0 0 10px var(--accent-neon)',
          zIndex: 10
        }}
      />
    );
  };

  return (
    <div className="glass-panel" style={{ position: 'relative', width: '100%', height: '450px', margin: '40px 0', overflow: 'hidden' }}>
      <h3 style={{ position: 'absolute', top: '20px', left: '20px', zIndex: 20 }}>
        Scenario: Devices are Far Apart
        <br/>
        <span style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>
          {step === 0 && "Waiting to send message..."}
          {step === 1 && "Sender connects to nearest peer..."}
          {step === 2 && "Message hops to Receiver!"}
          {step === 3 && "Oh no! A relay node dropped offline!"}
          {step === 4 && "Self-Healing: Mesh instantly reroutes!"}
        </span>
      </h3>

      {/* Connection Lines */}
      <svg style={{ position: 'absolute', width: '100%', height: '100%', top: 0, left: 0, zIndex: 1 }}>
        <line x1="10%" y1="50%" x2="35%" y2="20%" stroke="rgba(255,255,255,0.1)" strokeWidth="2" strokeDasharray="5,5" />
        <line x1="35%" y1="20%" x2="60%" y2="50%" stroke={step >= 3 ? "rgba(255,0,0,0.3)" : "rgba(255,255,255,0.1)"} strokeWidth="2" strokeDasharray="5,5" />
        <line x1="60%" y1="50%" x2="90%" y2="50%" stroke={step >= 3 ? "rgba(255,0,0,0.3)" : "rgba(255,255,255,0.1)"} strokeWidth="2" strokeDasharray="5,5" />
        <line x1="35%" y1="20%" x2="60%" y2="75%" stroke={step === 4 ? "rgba(0,240,255,0.3)" : "rgba(255,255,255,0.05)"} strokeWidth="2" strokeDasharray="5,5" />
        <line x1="60%" y1="75%" x2="90%" y2="50%" stroke={step === 4 ? "rgba(0,240,255,0.3)" : "rgba(255,255,255,0.05)"} strokeWidth="2" strokeDasharray="5,5" />
      </svg>

      {/* Nodes */}
      {nodes.map(node => (
        <motion.div
          key={node.id}
          animate={{
            scale: node.active ? 1.1 : 1,
            boxShadow: node.broken ? '0 0 15px rgba(255,0,0,0.5)' : (node.active ? '0 0 15px rgba(176,92,255,0.5)' : 'none')
          }}
          style={{
            position: 'absolute',
            left: `${node.x}%`,
            top: `${node.y}%`,
            transform: 'translate(-50%, -50%)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            zIndex: 5
          }}
        >
          <div style={{
            background: node.broken ? 'rgba(255,0,0,0.2)' : 'rgba(28,29,37,0.9)',
            border: `1px solid ${node.broken ? 'red' : (node.active ? 'var(--accent-purple)' : 'rgba(255,255,255,0.2)')}`,
            padding: '15px',
            borderRadius: '50%',
          }}>
            {node.broken ? <XCircle color="red" /> : <Smartphone color={node.active ? 'var(--accent-purple)' : 'gray'} />}
          </div>
          <span style={{ marginTop: '10px', fontSize: '0.8rem', color: node.broken ? 'red' : (node.active ? 'white' : 'gray') }}>{node.label}</span>
        </motion.div>
      ))}

      {renderPacket()}
      
      <button 
        onClick={() => setStep(0)} 
        style={{ position: 'absolute', bottom: '20px', right: '20px', background: 'transparent', border: '1px solid var(--accent-neon)', color: 'var(--accent-neon)', padding: '5px 10px', borderRadius: '5px', cursor: 'pointer', zIndex: 20, display: 'flex', alignItems: 'center', gap: '5px' }}
      >
        <RefreshCw size={14} /> Restart Animation
      </button>
    </div>
  );
}
