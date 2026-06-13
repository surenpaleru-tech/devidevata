import React, { useState, useEffect } from "react";

/**
 * DeityGallery
 * A highly responsive, visual, interactive gallery of supreme Indian Deities.
 * Uses Tailwind CSS for polished presentation, supports full search/filtering,
 * and opens an elegant overlay modal with mantras, attributes, and iconography details.
 */
export default function DeityGallery() {
  const [deities, setDeities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedDeity, setSelectedDeity] = useState(null);
  const [selectedAttribute, setSelectedAttribute] = useState("All");

  // Fetch the structured JSON data source.
  // Includes immediate fallback to static list if fetch fails (e.g., when run locally without a server).
  useEffect(() => {
    fetch("/deities.json")
      .then((res) => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then((data) => {
        setDeities(data);
        setLoading(false);
      })
      .catch((err) => {
        console.warn("Failed to fetch deities.json from server, loading integrated fallback database.", err);
        // Fallback dataset ensures app remains fully operational
        const fallbackDatabase = [
          {
            id: 1,
            name: "Lord Ganesha",
            sanskritName: "श्री गणेश",
            title: "Remover of Obstacles & Lord of Beginnings",
            description: "Ganesha is the patron of wisdom, intellect, arts, and sciences. With his distinctive elephant head, he is revered first in all sacred Vedic rituals as the supreme remover of all physical and spiritual obstacles (Vighnaharta).",
            attributes: ["Wisdom", "Success", "Intellect", "New Beginnings"],
            mantra: "ॐ गं गणपतये नमः (Om Gam Ganapataye Namaha)",
            colorTheme: "#FF9933",
            iconography: "Elephant head representing intellect, large ears to listen to prayers, a potbelly representing the assimilation of the cosmos, holding sweet modaks (rewards of spiritual sadhana), and riding his humble vehicle, a mouse (mushika).",
            image: "https://images.unsplash.com/photo-1567591905623-fa5097475141?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 2,
            name: "Lord Shiva",
            sanskritName: "शिव",
            title: "The Supreme Yogi & Cosmic Destroyer",
            description: "Shiva is the Adiyogi (the first yogi) and the supreme transformer within the Trimurti framework. He is the master of dance (Nataraja), representing both creative cosmic rhythm and ultimate dissolution.",
            attributes: ["Asceticism", "Limitation Dissolution", "Meditation", "Divine Dance"],
            mantra: "ॐ नमः शिवाय (Om Namah Shivaya)",
            colorTheme: "#008080",
            iconography: "Smeared with sacred ash (vibhuti), third eye representing wisdom, crescent moon in his matted locks, the sacred river Ganga flowing from his head, wrapped in tiger skin, holding the trident (trishul) and damru, with the serpent Vasuki coiled around his neck.",
            image: "https://images.unsplash.com/photo-1616046229478-9901c5536a45?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 3,
            name: "Lord Vishnu",
            sanskritName: "विष्णु",
            title: "The Infinite Preserver & Cosmic Protector",
            description: "Vishnu is the supreme deity responsible for supporting, sustaining, and preserving the cosmic order (Dharma). He incarnates on Earth as various avataras (such as Rama and Krishna) whenever moral order is in peril.",
            attributes: ["Sustenance", "Cosmic Order", "Compassion", "Reincarnation"],
            mantra: "ॐ नमो भगवते वासुदेवाय (Om Namo Bhagavate Vasudevaya)",
            colorTheme: "#4169E1",
            iconography: "Four arms carrying the Sudarshana Chakra (universal disc), Panchajanya (conch shell representing the primodial sound Om), Kaumodaki (gada/mace), and Padma (lotus representing unblemished beauty). Resting upon the multi-headed serpent Sheshnag.",
            image: "https://images.unsplash.com/photo-1590073844006-33379778ae09?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 4,
            name: "Devi Durga",
            sanskritName: "दुर्गा",
            title: "The Invincible Warrior Mother",
            description: "Durga is the supreme active feminine energy (Shakti) representing the combined power of all divine forces. She is the mother warrior who vanquished the demon Mahishasura, restoring righteousness and cosmic harmony.",
            attributes: ["Strength", "Protection", "Feminine Power", "Victory"],
            mantra: "ॐ दुं दुर्गायै नमः (Om Dum Durgayei Namaha)",
            colorTheme: "#D2143A",
            iconography: "Riding a powerful lion, holding ten weapons gifted by the gods in her ten hands (including Shiva's trident, Vishnu's chakra, and Indra's thunderbolt), radiating a bright golden glow representing invincible courage.",
            image: "https://images.unsplash.com/photo-1609137144813-7d7bceda287a?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 5,
            name: "Devi Lakshmi",
            sanskritName: "लक्ष्मी",
            title: "Goddess of Abundance & Spiritual Wealth",
            description: "Lakshmi is the divine consort of Lord Vishnu, representing outer and inner wealth, prosperity, purity, and fortune. She bestows both physical abundance and spiritual enlightenment on sincere devotees.",
            attributes: ["Prosperity", "Wealth", "Purity", "Grace"],
            mantra: "ॐ श्रीं महालक्ष्म्यै नमः (Om Shreem Maha Lakshmyai Namaha)",
            colorTheme: "#FF1493",
            iconography: "Standing or sitting on a fully bloomed pink lotus, with gold coins showering from her open palms representing flow of cosmic grace, flanked by divine white elephants (gaja) pouring sacred water.",
            image: "https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 6,
            name: "Devi Saraswati",
            sanskritName: "सरस्वती",
            title: "Goddess of Wisdom, Music & Fine Arts",
            description: "Saraswati represents the free-flowing river of consciousness, speech, knowledge, wisdom, and fine arts. Sincere students, classical musicians, and philosophers worship her to gain absolute clarity of mind.",
            attributes: ["Knowledge", "Creativity", "Fine Arts", "Eloquence"],
            mantra: "ॐ ऐं सरस्वत्यै नमः (Om Eim Saraswatyai Namaha)",
            colorTheme: "#999999",
            iconography: "Clad in a pure, spotless white saree representing absolute purity of intellect, seated on a white rock or swan (hansa), holding the Veena (stringed musical instrument), a book (Vedas), and a crystal mala (concentration).",
            image: "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 7,
            name: "Lord Krishna",
            sanskritName: "कृष्ण",
            title: "The Master of Love, Devotion & Ultimate Truth",
            description: "Krishna is the central avatar of Lord Vishnu, widely beloved for his divine teachings in the Bhagavad Gita. He represents pure bliss, cosmic playfulness (Lila), and is a guide of supreme wisdom.",
            attributes: ["Unconditional Love", "Supreme Wisdom", "Playfulness", "Divine Guidance"],
            mantra: "हरे कृष्ण हरे कृष्ण कृष्ण कृष्ण हरे हरे (Hare Krishna Mantra)",
            colorTheme: "#1E90FF",
            iconography: "Golden-dark blue skin representing infinite space, wearing a peacock feather in his crown, playing a melodious bamboo flute, frequently accompanied by cows, calves, and loving Gopis.",
            image: "https://images.unsplash.com/photo-1597113366853-fc19207ec7a7?auto=format&fit=crop&q=80&w=600"
          },
          {
            id: 8,
            name: "Lord Hanuman",
            sanskritName: "हनुमान",
            title: "The Embodiment of Devotion & Selfless Strength",
            description: "Hanuman is a great devotee of Lord Rama and an incarnation of Shiva's power (Rudra). He represents the absolute submission of the ego to absolute devotion, generating limitless strength, courage, and loyalty.",
            attributes: ["Devotion", "Humility", "Immense Strength", "Protection"],
            mantra: "ॐ हनुमते नमः (Om Hanumate Namaha)",
            colorTheme: "#FF4500",
            iconography: "Stout, powerful body holding a heavy golden Gada (mace) signifying strength, hands joined in profound prayer, sometimes tearing open his chest to reveal the images of Lord Rama and Devi Sita inside his heart.",
            image: "https://images.unsplash.com/photo-1582234327421-dfd1933ccf03?auto=format&fit=crop&q=80&w=600"
          }
        ];
        setDeities(fallbackDatabase);
        setLoading(false);
      });
  }, []);

  // Gather all unique attributes for target filter tags
  const allAttributes = ["All", ...new Set(deities.flatMap((d) => d.attributes || []))];

  // Filter items matching current query and selected tag
  const filteredDeities = deities.filter((deity) => {
    const matchesSearch =
      deity.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      deity.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      deity.sanskritName.includes(searchQuery) ||
      deity.description.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesAttribute =
      selectedAttribute === "All" ||
      (deity.attributes && deity.attributes.includes(selectedAttribute));

    return matchesSearch && matchesAttribute;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans selection:bg-amber-500 selection:text-slate-950">
      
      {/* 1. Divine Aesthetic Header Section */}
      <header className="relative py-12 px-6 text-center border-b border-amber-500/10 overflow-hidden bg-slate-950">
        {/* Spiritual Ambient Background Glows */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-80 h-80 bg-orange-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-1/4 w-64 h-64 bg-yellow-500/5 rounded-full blur-3xl pointer-events-none" />

        <div className="relative max-w-4xl mx-auto">
          <div className="inline-block text-amber-500 text-xs font-semibold tracking-widest uppercase mb-3 px-3 py-1 bg-amber-500/5 rounded-full border border-amber-500/20">
            Sanatana Dharma Pantheon
          </div>
          <h1 className="text-4xl md:text-5xl font-extrabold font-serif tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-orange-400 to-amber-200">
            Devi Devata Pantheon
          </h1>
          <p className="mt-4 text-sm md:text-base text-slate-400 leading-relaxed max-w-2xl mx-auto">
            Explore the avatars, cosmology, sacred representations, divine mantras, and spiritual roles of supreme deities with our interactive responsive collection.
          </p>
        </div>
      </header>

      {/* 2. Interactive Search & Filters Section */}
      <section className="max-w-7xl mx-auto px-4 md:px-8 py-8">
        <div className="flex flex-col md:flex-row gap-4 items-center justify-between bg-slate-900/60 p-4 rounded-2xl border border-slate-800/80 backdrop-blur-md mb-8">
          
          {/* Dynamic Search Box */}
          <div className="relative w-full md:w-96">
            <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="h-5 w-5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </span>
            <input
              type="text"
              placeholder="Search by name, Sanskrit mantra, attributes..."
              className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl focus:border-amber-500/60 focus:outline-none focus:ring-1 focus:ring-amber-500/40 text-sm placeholder:text-slate-500 transition-all text-slate-100"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {/* Quick attribute tags filter */}
          <div className="w-full md:w-auto flex items-center gap-2 overflow-x-auto scroller-hidden py-1">
            <span className="text-xs text-amber-500/80 font-semibold tracking-wider uppercase whitespace-nowrap hidden lg:inline mr-2">
              Filter:
            </span>
            <div className="flex gap-2">
              {allAttributes.map((attr) => (
                <button
                  key={attr}
                  onClick={() => setSelectedAttribute(attr)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold tracking-wide border transition-all duration-200 whitespace-nowrap ${
                    selectedAttribute === attr
                      ? "bg-amber-500 text-slate-950 border-amber-500 shadow-md shadow-amber-500/15"
                      : "bg-slate-950/40 text-slate-400 border-slate-800 hover:text-slate-100 hover:border-slate-700"
                  }`}
                >
                  {attr}
                </button>
              ))}
            </div>
          </div>

        </div>

        {/* 3. Responsive Deities Grid */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="w-12 h-12 border-4 border-amber-500/25 border-t-amber-500 rounded-full animate-spin"></div>
            <p className="mt-4 text-xs text-amber-500/80 tracking-widest animate-pulse uppercase font-medium">
              Invoking Divine Presence...
            </p>
          </div>
        ) : filteredDeities.length === 0 ? (
          <div className="text-center py-24 bg-slate-900/10 rounded-2xl border border-dashed border-slate-800">
            <svg className="mx-auto h-12 w-12 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h3 className="mt-4 text-base font-semibold text-slate-300">No divine match found</h3>
            <p className="mt-2 text-xs text-slate-500 max-w-sm mx-auto">
              We couldn't expand any deity matching "{searchQuery}". Try refining your spelling or select other filter tags.
            </p>
            <button
              onClick={() => { setSearchQuery(""); setSelectedAttribute("All"); }}
              className="mt-6 inline-flex items-center gap-1.5 px-4 py-2 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 text-xs font-bold tracking-wider uppercase border border-amber-500/20 rounded-lg transition-all"
            >
              Reset Search Filter
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredDeities.map((deity) => (
              <div
                key={deity.id}
                onClick={() => setSelectedDeity(deity)}
                className="group relative cursor-pointer backdrop-blur-md bg-slate-950/45 border border-slate-800/60 hover:bg-slate-900/40 hover:border-amber-500/40 rounded-2xl overflow-hidden transition-all duration-500 ease-out hover:-translate-y-2.5 hover:shadow-2xl hover:shadow-amber-500/10"
              >
                {/* 1. Dynamic Glass Shimmer Sheen Reflection */}
                <div className="absolute inset-0 w-[250%] -left-full bg-gradient-to-r from-transparent via-white/[0.06] to-transparent -skew-x-12 transition-transform duration-1000 group-hover:translate-x-[150%] pointer-events-none ease-out" />
                
                {/* 2. Glassmorphic Radial Amber/Orange Cosmic Aura Background */}
                <div className="absolute inset-0 bg-gradient-to-br from-amber-500/0 via-transparent to-orange-500/0 group-hover:from-amber-500/5 group-hover:to-orange-500/5 transition-all duration-700 pointer-events-none" />

                {/* Visual Image container with theme gradient cover */}
                <div className="relative h-52 overflow-hidden bg-slate-950 border-b border-slate-800/30">
                  <img
                    src={deity.image}
                    alt={deity.name}
                    className="w-full h-full object-cover filter brightness-[0.88] contrast-[0.95] group-hover:brightness-100 group-hover:contrast-100 transition-all duration-700 ease-out group-hover:scale-110"
                    onError={(e) => {
                      // Fallback image in case of load failure
                      e.target.src = "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&q=80&w=600";
                    }}
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/20 to-transparent" />
                  
                  {/* Sanskrit text floating top right - now with custom hover glow */}
                  <span className="absolute top-3 right-3 text-xs bg-slate-950/90 text-amber-500/90 border border-amber-500/20 font-serif px-2.5 py-1 rounded-lg backdrop-blur-md shadow-md transition-all duration-500 group-hover:text-amber-400 group-hover:border-amber-500/40 group-hover:shadow-[0_0_12px_rgba(245,158,11,0.25)]">
                    {deity.sanskritName}
                  </span>
                </div>

                {/* Content Section */}
                <div className="p-5 flex flex-col justify-between min-h-[170px] relative z-10">
                  <div>
                    <h3 className="text-lg font-bold font-serif text-slate-100 group-hover:text-amber-400 transition-colors duration-500">
                      {deity.name}
                    </h3>
                    <p className="text-xs text-amber-500/80 font-medium tracking-wide mt-1.5 transition-colors duration-500 group-hover:text-amber-400/90">
                      {deity.title}
                    </p>
                    <p className="mt-3 text-xs text-slate-400 line-clamp-3 leading-relaxed transition-colors duration-500 group-hover:text-slate-300">
                      {deity.description}
                    </p>
                  </div>

                  {/* Attributes Tags Row */}
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {deity.attributes.slice(0, 3).map((attr) => (
                      <span
                        key={attr}
                        className="text-[10px] backdrop-blur-sm bg-slate-950/50 text-slate-400 border border-slate-800/50 px-2 py-0.5 rounded-md transition-all duration-300 group-hover:border-amber-500/20 group-hover:text-amber-300 group-hover:bg-slate-950/80"
                      >
                        {attr}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Corner Saffron Ribbon Accent */}
                <div className="absolute bottom-0 inset-x-0 h-[2px] bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-400 transform scale-x-0 group-hover:scale-x-100 transition-transform duration-500 ease-out" />
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 4. Detailed Overlay Sheet (Aura-Guided Modal Box) */}
      {selectedDeity && (
        <div className="fixed inset-0 z-50 overflow-y-auto flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-lg animate-fade-in">
          
          {/* Modal Container */}
          <div className="relative w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-3xl overflow-hidden shadow-2xl shadow-amber-500/5 max-h-[90vh] flex flex-col">
            
            {/* Header banner image */}
            <div className="relative h-64 md:h-72 w-full flex-shrink-0">
              <img
                src={selectedDeity.image}
                alt={selectedDeity.name}
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-slate-900 via-slate-900/40 to-transparent" />
              
              {/* Back close button in top right of the modal banner */}
              <button
                onClick={() => setSelectedDeity(null)}
                className="absolute top-4 right-4 p-2 bg-slate-950/80 border border-slate-800 text-slate-400 hover:text-slate-100 hover:border-slate-600 rounded-full backdrop-blur-md shadow-md transition-all"
                aria-label="Close details"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>

              <div className="absolute bottom-4 left-6 right-6">
                <span className="text-2xl font-serif text-amber-400 block mb-1">
                  {selectedDeity.sanskritName}
                </span>
                <h2 className="text-3xl font-extrabold font-serif text-slate-50 tracking-tight">
                  {selectedDeity.name}
                </h2>
                <p className="text-sm text-amber-500/90 font-medium">
                  {selectedDeity.title}
                </p>
              </div>
            </div>

            {/* Scrollable details contents */}
            <div className="p-6 md:p-8 overflow-y-auto space-y-6">
              
              {/* Overview text */}
              <div className="space-y-2">
                <h4 className="text-xs text-amber-500 font-bold tracking-widest uppercase">
                  Sacred Significance
                </h4>
                <p className="text-slate-300 text-sm md:text-base leading-relaxed">
                  {selectedDeity.description}
                </p>
              </div>

              {/* Mantra display block */}
              <div className="bg-slate-950/80 p-5 rounded-2xl border border-amber-500/10 relative overflow-hidden">
                <div className="absolute inset-y-0 right-0 w-32 bg-gradient-to-l from-orange-500/5 to-transparent pointer-events-none" />
                <h4 className="text-xs text-amber-500 font-bold tracking-widest uppercase mb-2">
                  Revered Devotional Mantra
                </h4>
                <p className="text-lg font-serif text-amber-200 tracking-wide leading-relaxed py-1.5 select-all">
                  {selectedDeity.mantra}
                </p>
                <div className="flex justify-between items-center mt-3 text-[11px] text-slate-500">
                  <span>Chanted to invoke cosmic energy & mental composure</span>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(selectedDeity.mantra);
                      alert("Mantra copied to clipboard safely!");
                    }}
                    className="text-amber-500 hover:text-amber-400 font-bold tracking-wider uppercase flex items-center gap-1 transition-colors"
                  >
                    Copy Mantra
                  </button>
                </div>
              </div>

              {/* Grid: Attributes & Iconography details */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
                
                {/* Iconography */}
                <div className="space-y-2">
                  <h4 className="text-xs text-amber-500 font-bold tracking-widest uppercase">
                    Iconography & Symbols
                  </h4>
                  <p className="text-slate-400 text-xs md:text-sm leading-relaxed">
                    {selectedDeity.iconography}
                  </p>
                </div>

                {/* Attributes full list */}
                <div className="space-y-3">
                  <h4 className="text-xs text-amber-500 font-bold tracking-widest uppercase">
                    Attributes & Realms
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedDeity.attributes.map((attr) => (
                      <span
                        key={attr}
                        className="px-3 py-1 bg-slate-950/50 border border-slate-800 text-xs text-slate-300 rounded-lg hover:border-amber-500/20 transition-all font-medium"
                      >
                        🌟 {attr}
                      </span>
                    ))}
                  </div>
                  <div className="text-[11px] text-slate-500 italic mt-2">
                    Part of the structured JSON schema mapping core energies of Hindu cosmology.
                  </div>
                </div>

              </div>

            </div>

            {/* Back action bar footer */}
            <div className="p-4 bg-slate-950/70 border-t border-slate-850 text-right flex justify-end gap-3 flex-shrink-0">
              <button
                onClick={() => setSelectedDeity(null)}
                className="px-5 py-2 bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-slate-100 rounded-xl text-xs font-bold tracking-wider uppercase transition-all"
              >
                Close Details
              </button>
            </div>

          </div>
        </div>
      )}

      {/* 5. Clean Footer Info */}
      <footer className="mt-16 py-8 border-t border-slate-900 text-center text-xs text-slate-600 max-w-7xl mx-auto px-6">
        <p>© 2026 DeviDevata Applet. Built with standard-compliant responsive React & structured JSON storage.</p>
        <p className="mt-1.5 opacity-60">Serving visual representations, mantras, and cultural teachings of Indian deities from centralized repositories.</p>
      </footer>

    </div>
  );
}
