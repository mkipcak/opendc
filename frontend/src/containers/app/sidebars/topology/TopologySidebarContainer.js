import { connect } from 'react-redux'
import TopologySidebarComponent from '../../../../components/app/sidebars/topology/TopologySidebarComponent'

const mapStateToProps = (state) => {
    return {
        interactionLevel: state.interactionLevel,
    }
}

const TopologySidebarContainer = connect(mapStateToProps)(TopologySidebarComponent)

export default TopologySidebarContainer
